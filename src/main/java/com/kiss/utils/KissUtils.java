package com.kiss.utils;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class KissUtils {

    public static boolean isSamePlayer(ServerPlayerEntity p1, ServerPlayerEntity p2) {
        return p1.getUuid().equals(p2.getUuid());
    }

    public static boolean hasVisibleNearbyPlayers(ServerPlayerEntity player, ServerWorld world, double radius, double maxViewAngleDegree) {
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
        if (!observer.getServerWorld().equals(target.getServerWorld())) return false;
        if (!observer.canSee(target)) return false;

        Vec3d lookVec = observer.getRotationVec(1.0F).normalize();
        Vec3d dirToTarget = target.getPos().subtract(observer.getPos()).normalize();

        double dot = lookVec.dotProduct(dirToTarget);
        double angleRad = Math.acos(dot);
        double maxAngleRad = Math.toRadians(maxAngleDegree);

        return angleRad <= maxAngleRad;
    }

    public static void spawnHeartParticles(ServerWorld world, ServerPlayerEntity player, int count, double offset, double speed) {
        Vec3d lookVec = player.getRotationVec(1.0F).multiply(0.3);
        Vec3d pos = player.getPos();
        double x = pos.x + lookVec.x;
        double y = pos.y + 1.62;
        double z = pos.z + lookVec.z;
        world.spawnParticles(ParticleTypes.HEART, x, y, z, count, offset, offset, offset, speed);
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
