package com.kiss.utils;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class KissUtils {
    private KissUtils() {}

    public static boolean isSamePlayer(ServerPlayerEntity p1, ServerPlayerEntity p2) {
        return p1.getUuid().equals(p2.getUuid());
    }

    public static Entity getNearestAffectedMob(ServerPlayerEntity player, ServerWorld world, int radius, Set<EntityType<?>> affectedMobs) {
        Box box = Box.from(player.getPos()).expand(radius);
        Predicate<Entity> predicate = entity ->
                affectedMobs.contains(entity.getType()) &&
                        !entity.isInvisible() &&
                        player.canSee(entity);

        List<Entity> nearbyMobs = world.getOtherEntities(player, box, predicate);

        if (nearbyMobs.isEmpty()) {
            return null;
        }

        // Find the nearest mob
        Vec3d playerPos = player.getPos();
        return nearbyMobs.stream()
                .min(Comparator.comparingDouble(mob -> mob.getPos().distanceTo(playerPos)))
                .orElse(null);
    }

    public static boolean isPlayerInvisible(ServerPlayerEntity player) {
        return (player.isSpectator() || player.hasStatusEffect(StatusEffects.INVISIBILITY));
    }

    public static boolean hasVisibleNearbyPlayers(ServerPlayerEntity player, ServerWorld world, double radius, double maxViewAngleDegree) {
        if (isPlayerInvisible(player)) return false;

        UUID self = player.getUuid();
        Vec3d pos = player.getPos();
        Box area = new Box(pos.add(-radius, -6, -radius), pos.add(radius, 6, radius));

        for (ServerPlayerEntity other : world.getPlayers()) {
            if (!other.getUuid().equals(self)
                    && area.contains(other.getPos())
                    && KissUtils.isPlayerInViewAndCanSee(player, other, maxViewAngleDegree)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerInViewAndCanSee(ServerPlayerEntity observer, ServerPlayerEntity target, double maxAngleDegree) {
        if (!observer.getWorld().equals(target.getWorld())) return false;
        if (!observer.canSee(target)) return false;

        Vec3d lookVec = observer.getRotationVec(1.0F).normalize();
        Vec3d dirToTarget = target.getPos().subtract(observer.getPos()).normalize();

        double dot = lookVec.dotProduct(dirToTarget);
        double angleRad = Math.acos(dot);
        double maxAngleRad = Math.toRadians(maxAngleDegree);

        return angleRad <= maxAngleRad;
    }

    public static void spawnHeartParticlesForPlayer(ServerWorld world, ServerPlayerEntity player, int count, double offset, double speed) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F).multiply(0.3);

        float modelHeight = player.getHeight();
        double sizeFactor = MathHelper.clamp(modelHeight / 1.8f, 0.5, 2.0);

        int adjustedCount = (int)(count * sizeFactor);
        double adjustedOffset = offset * sizeFactor;
        double adjustedSpeed = speed * (0.8 + sizeFactor * 0.2);

        world.spawnParticles(
                ParticleTypes.HEART,
                eyePos.x + lookVec.x,
                eyePos.y,
                eyePos.z + lookVec.z,
                adjustedCount,
                adjustedOffset * 0.6,
                adjustedOffset * 0.3,
                adjustedOffset * 0.6,
                adjustedSpeed
        );
    }

    public static void spawnHeartParticles(ServerWorld world, Entity entity, int count, double yMargin,  double offset, double speed) {
        Vec3d pos = entity.getPos();

        double height = entity.getHeight();
        double yOffset = height + yMargin;

        if (entity.hasVehicle() && entity.getVehicle() != null) {
            yOffset += entity.getVehicle().getHeight();
        }

        world.spawnParticles(
                ParticleTypes.HEART,
                pos.x,
                pos.y + yOffset,
                pos.z,
                count,
                offset, offset, offset,
                speed
        );
    }

    public static void sendError(ServerCommandSource source, String message, Formatting color) {
        source.sendError(Text.literal(message).formatted(color));
    }

    public static MutableText buildMessage(String template, Text playerName, Formatting color) {
        try {
            String[] parts = template.split("%s", -1);
            MutableText result = Text.literal(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                result = result.append(playerName).append(Text.literal(parts[i]));
            }
            return result.formatted(color);
        } catch (Exception e) {
            return Text.literal(playerName.getString() + " ❤").formatted(color);
        }
    }
}
