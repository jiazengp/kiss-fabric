package com.kiss.manager;

import com.kiss.config.KissConfig;
import com.kiss.data.PlayerSneakData;
import com.kiss.utils.KissUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SneakManager {
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
            EntityType.DOLPHIN,

            EntityType.HAPPY_GHAST
    );


    public SneakManager(KissConfig kissConfig) {
        this.config = kissConfig;
    }

    public void onEndServerTick(MinecraftServer server, int tick) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            handleSneak(player, tick);
        }
    }

    public void onPlayerLeave(UUID uuid) {
        dataMap.remove(uuid);
    }

    private void handleSneak(ServerPlayerEntity player, int currentTick) {
        if (KissUtils.isPlayerInvisible(player)) return;

        UUID id = player.getUuid();
        PlayerSneakData d = dataMap.computeIfAbsent(id, unused -> new PlayerSneakData());

        boolean nowSneaking = player.isSneaking();

        if (nowSneaking && !d.wasSneaking()) processSneakToggle(player, currentTick, d);
        if (!nowSneaking && d.wasSneaking()) d.decreaseParticleLevel(config.sneakParticleDecay);

        d.setWasSneaking(nowSneaking);
    }

    /** 第一次侦测到新的蹲下动作 **/
    private void processSneakToggle(ServerPlayerEntity player, int tick, PlayerSneakData playerSneakData) {
        int delta = tick - playerSneakData.lastTick();
        playerSneakData.setLastTick(tick);
        playerSneakData.setCount(delta <= config.sneakTimeWindow ? playerSneakData.count() + 1 : 1);

        boolean triggered = playerSneakData.count() >= config.sneakTriggerCount;
        Entity nearest = KissUtils.getNearestAffectedMob(player, player.getWorld(),
                config.mobTriggerRadius, DEFAULT_AFFECTED_MOBS);

        if (triggered) {
            playerSneakData.increaseParticleLevel(config.maxSneakParticles);

            spawnHeartParticles(player, Math.clamp(config.sneakTriggerRadius / 2, 2, 16), playerSneakData.particleLevel() + 1, 0.3, 0.1);
            if (nearest != null) spawnHeartParticles(player.getWorld(), nearest, playerSneakData.particleLevel(), -0.2, 0.3, 0.1);

            playerSneakData.resetCount();
        } else {
            spawnHeartParticles(player, config.sneakTriggerRadius, 1, 0.1, 0.05);
            if (nearest != null) spawnHeartParticles(player.getWorld(), nearest, 1, -0.2, 0.3, 0.1);
        }
    }

    private void spawnHeartParticles(ServerPlayerEntity player, double sneakTriggerRadius, int amount, double... offsets) {
        if (KissUtils.hasVisibleNearbyPlayers(player, player.getWorld(),
                sneakTriggerRadius, config.maxViewAngleDegree)) {
            KissUtils.spawnHeartParticlesForPlayer(player.getWorld(), player, amount,
                    offsets[0], offsets[1]);
        }
    }

    private static void spawnHeartParticles(ServerWorld world, Entity e, int amount, double... offsets) {
        KissUtils.spawnHeartParticles(world, e, amount, offsets[0], offsets[1], offsets[2]);
    }
}
