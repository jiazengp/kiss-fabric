package com.kiss.utils;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class KissUtils {
    private KissUtils() {}

    public static boolean isSamePlayer(ServerPlayer p1, ServerPlayer p2) {
        return p1.getUUID().equals(p2.getUUID());
    }

    public static Entity getNearestAffectedMob(ServerPlayer player, ServerLevel world, int radius, Set<EntityType<?>> affectedMobs) {
        AABB box = AABB.unitCubeFromLowerCorner(player.position()).inflate(radius);
        Predicate<Entity> predicate = entity ->
                affectedMobs.contains(entity.getType()) &&
                        !entity.isInvisible() &&
                        player.hasLineOfSight(entity);

        List<Entity> nearbyMobs = world.getEntities(player, box, predicate);

        if (nearbyMobs.isEmpty()) {
            return null;
        }

        // Find the nearest mob
        Vec3 playerPos = player.position();
        return nearbyMobs.stream()
                .min(Comparator.comparingDouble(mob -> mob.position().distanceTo(playerPos)))
                .orElse(null);
    }

    public static boolean isPlayerInvisible(ServerPlayer player) {
        return (player.isSpectator() || player.hasEffect(MobEffects.INVISIBILITY) || FakePlayerHelper.isFakePlayer(player));
    }

    public static boolean hasVisibleNearbyPlayers(ServerPlayer player, ServerLevel world, double radius, double maxViewAngleDegree) {
        if (isPlayerInvisible(player)) return false;

        UUID self = player.getUUID();
        Vec3 pos = player.position();
        AABB area = new AABB(pos.add(-radius, -6, -radius), pos.add(radius, 6, radius));

        for (ServerPlayer other : world.players()) {
            if (!other.getUUID().equals(self)
                    && area.contains(other.position())
                    && KissUtils.isPlayerInViewAndCanSee(player, other, maxViewAngleDegree)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerInViewAndCanSee(ServerPlayer observer, ServerPlayer target, double maxAngleDegree) {
        if (!observer.level().equals(target.level())) return false;
        if (!observer.hasLineOfSight(target)) return false;

        Vec3 lookVec = observer.getViewVector(1.0F).normalize();
        Vec3 dirToTarget = target.position().subtract(observer.position()).normalize();

        double dot = lookVec.dot(dirToTarget);
        double angleRad = Math.acos(dot);
        double maxAngleRad = Math.toRadians(maxAngleDegree);

        return angleRad <= maxAngleRad;
    }

    public static void spawnHeartParticlesForPlayer(ServerLevel world, ServerPlayer player, int count, double offset, double speed) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F).scale(0.3);

        float modelHeight = player.getBbHeight();
        double sizeFactor = Mth.clamp(modelHeight / 1.8f, 0.5, 2.0);

        int adjustedCount = (int)(count * sizeFactor);
        double adjustedOffset = offset * sizeFactor;
        double adjustedSpeed = speed * (0.8 + sizeFactor * 0.2);

        world.sendParticles(
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

    public static void spawnHeartParticles(ServerLevel world, Entity entity, int count, double yMargin, double offset, double speed) {
        Vec3 pos = entity.position();

        double height = entity.getBbHeight();
        double yOffset = height + yMargin;

        if (entity.isPassenger() && entity.getVehicle() != null) {
            yOffset += entity.getVehicle().getBbHeight();
        }

        world.sendParticles(
                ParticleTypes.HEART,
                pos.x,
                pos.y + yOffset,
                pos.z,
                count,
                offset, offset, offset,
                speed
        );
    }
}
