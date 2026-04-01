package com.kiss.manager;

import com.kiss.config.KissConfig;
import com.kiss.data.PlayerSneakData;
import com.kiss.utils.KissUtils;
import com.kiss.utils.PermissionChecker;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SneakManager {
    private int lastCheckedTick = 0;

    private final KissConfig config;
    private final Map<UUID, PlayerSneakData> dataMap = new Object2ObjectOpenHashMap<>();

    private static final Set<EntityType<?>> DEFAULT_AFFECTED_MOBS = Set.of(
            // 驯服型 / 中立友好
            EntityType.WOLF,
            EntityType.CAT,
            EntityType.OCELOT,
            EntityType.HORSE,
            EntityType.DONKEY,
            EntityType.MULE,
            EntityType.LLAMA,
            EntityType.TRADER_LLAMA,
            EntityType.CAMEL,
            EntityType.PARROT,
            EntityType.FOX,
            EntityType.PANDA,
            EntityType.BEE,

            // 农场类
            EntityType.SHEEP,
            EntityType.PIG,
            EntityType.COW,
            EntityType.CHICKEN,
            EntityType.RABBIT,
            EntityType.TURTLE,
            EntityType.MOOSHROOM,
            EntityType.SNIFFER,

            // 傀儡
            EntityType.IRON_GOLEM,
            EntityType.SNOW_GOLEM,

            // 水生
            EntityType.AXOLOTL,
            EntityType.DOLPHIN

//            EntityType.HAPPY_GHAST
    );


    public SneakManager(KissConfig kissConfig) {
        this.config = kissConfig;
    }

    public void onEndServerTick(MinecraftServer server, int tick) {
        if ((tick - lastCheckedTick) < config.sneakCheckInterval) return;
        lastCheckedTick = tick;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            handleSneak(player, tick);
        }
    }

    public void onPlayerLeave(UUID uuid) {
        dataMap.remove(uuid);
    }

    private void handleSneak(ServerPlayer player, int currentTick) {
        if (KissUtils.isPlayerInvisible(player)) return;
        if (!PermissionChecker.hasPermission(player, "trigger", PermissionLevel.ALL)) return;

        UUID id = player.getUUID();
        PlayerSneakData d = dataMap.computeIfAbsent(id, unused -> new PlayerSneakData());

        boolean nowSneaking = player.isShiftKeyDown();

        if (nowSneaking && !d.wasSneaking()) processSneakToggle(player, currentTick, d);
        if (!nowSneaking && d.wasSneaking()) d.decreaseParticleLevel(config.sneakParticleDecay);

        d.setWasSneaking(nowSneaking);
    }

    private void processSneakToggle(ServerPlayer player, int tick, PlayerSneakData playerSneakData) {
        int delta = tick - playerSneakData.lastTick();
        playerSneakData.setLastTick(tick);
        playerSneakData.setCount(delta <= config.sneakTimeWindow ? playerSneakData.count() + 1 : 1);

        boolean triggered = playerSneakData.count() >= config.sneakTriggerCount;
        Entity nearest = KissUtils.getNearestAffectedMob(player, player.level(),
                config.mobTriggerRadius, DEFAULT_AFFECTED_MOBS);
        boolean hasLongSneakingPlayer = allNearbyPlayersAreLongSneaking(player, config.mobTriggerRadius, tick, config.sneakLongThreshold);

        if (triggered && !hasLongSneakingPlayer) {
            playerSneakData.increaseParticleLevel(config.maxSneakParticles);

            spawnHeartParticles(player, Math.clamp(config.sneakTriggerRadius / 2, 2, 16), playerSneakData.particleLevel() + 1, 0.3, 0.1);
            if (nearest != null) spawnHeartParticles(player.level(), nearest, playerSneakData.particleLevel(), -0.2, 0.3, 0.1);

            playerSneakData.resetCount();
        } else {
            if (!hasLongSneakingPlayer) spawnHeartParticles(player, config.sneakTriggerRadius, 1, 0.1, 0.05);
            if (nearest != null) spawnHeartParticles(player.level(), nearest, 1, -0.2, 0.3, 0.1);
        }
    }

    private boolean allNearbyPlayersAreLongSneaking(ServerPlayer self, double radius, int currentTick, int longSneakThreshold) {
        double radiusSq = radius * radius;
        ServerLevel world = self.level();

        for (ServerPlayer other : world.players()) {
            if (other == self || other.isSpectator()) continue;
            if (self.distanceToSqr(other) > radiusSq) continue;

            PlayerSneakData data = dataMap.get(other.getUUID());
            if (data == null) continue;

            boolean sneaking = other.isShiftKeyDown();
            boolean longSneaking = (currentTick - data.lastTick()) >= longSneakThreshold;

            if (!sneaking || !longSneaking) {
                return false;
            }
        }

        return true;
    }

    private void spawnHeartParticles(ServerPlayer player, double sneakTriggerRadius, int amount, double... offsets) {
        if (KissUtils.hasVisibleNearbyPlayers(player, player.level(),
                sneakTriggerRadius, config.maxViewAngleDegree)) {
            KissUtils.spawnHeartParticlesForPlayer(player.level(), player, amount,
                    offsets[0], offsets[1]);
        }
    }

    private static void spawnHeartParticles(ServerLevel world, Entity e, int amount, double... offsets) {
        KissUtils.spawnHeartParticles(world, e, amount, offsets[0], offsets[1], offsets[2]);
    }
}
