package com.kissmod;

import com.kissmod.config.KissConfigs;
import com.kissmod.config.KissServerConfig;
import com.kissmod.platform.Services;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class KissModCommon {
  private static final Map<UUID, SneakData> playerSneakData = new HashMap<>();

  public static void init() {
    Constants.LOG.info(
        "Hello from KissMod Common init on {}! We are currently in a {} environment!",
        Services.PLATFORM.getPlatformName(),
        Services.PLATFORM.getEnvironmentName());

    if (Services.PLATFORM.isModLoaded("kissmod")) {
      Constants.LOG.info("KissMod is loaded!");
    }

    // Register event handlers
    Services.KISS_EVENT.registerServerTickHandler();
    Services.KISS_EVENT.registerCommandHandler();
  }

  public static void onServerTick(ServerLevel level) {
    removeOfflinePlayerState(level);
    for (ServerPlayer player : level.players()) {
      handleSneak(player, level);
    }
  }

  private static void removeOfflinePlayerState(ServerLevel level) {
    playerSneakData
        .keySet()
        .removeIf(uuid -> level.getServer().getPlayerList().getPlayer(uuid) == null);
    KissCommand.purgeExpiredCooldowns(KissConfigs.server());
  }

  private static void handleSneak(ServerPlayer player, ServerLevel level) {
    if (KissUtils.isExcludedPlayer(player) || !Services.PLATFORM.hasPermission(player, "trigger")) {
      playerSneakData.remove(player.getUUID());
      return;
    }

    KissServerConfig cfg = KissConfigs.server();
    UUID uuid = player.getUUID();
    SneakData data = playerSneakData.computeIfAbsent(uuid, u -> new SneakData());
    boolean isSneaking = player.isCrouching();

    if (isSneaking && !data.wasSneaking) {
      long delta = level.getGameTime() - data.lastTime;
      data.count =
          (data.lastTime >= 0 && delta <= cfg.sneakTriggerWindowTicks()) ? data.count + 1 : 1;
      data.lastTime = level.getGameTime();

      List<LivingEntity> friendlyEntities =
          cfg.enableEntityParticles()
              ? KissUtils.getFriendlyEntitiesNearby(player, level, cfg.entityParticleRadius())
              : List.of();
      boolean hasNearbyFriendlyEntities = !friendlyEntities.isEmpty();

      if (data.count >= cfg.sneakTriggerCount()) {
        boolean hasNearbyPlayers =
            KissUtils.hasVisibleNearbyPlayers(
                player, level, cfg.sneakTriggerRadius(), cfg.maxViewAngle());

        if (hasNearbyPlayers || hasNearbyFriendlyEntities) {
          data.particleLevel = Math.min(cfg.maxSneakParticles(), data.particleLevel + 1);
          if (hasNearbyPlayers && data.particleLevel > 0) {
            KissUtils.spawnHeartParticles(
                level, player, HeartParticleEffect.playerTrigger(data.particleLevel));
          }

          spawnFriendlyEntityHeartParticles(friendlyEntities, HeartParticleEffect.FRIENDLY_TRIGGER);
        }
        data.count = 0;
      } else {
        boolean hasNearbyPlayers =
            KissUtils.hasVisibleNearbyPlayers(
                player, level, cfg.sneakPreviewRadius(), cfg.maxViewAngle());

        if (hasNearbyPlayers || hasNearbyFriendlyEntities) {
          if (hasNearbyPlayers) {
            KissUtils.spawnHeartParticles(level, player, HeartParticleEffect.SNEAK_PREVIEW);
          }

          spawnFriendlyEntityHeartParticles(friendlyEntities, HeartParticleEffect.SNEAK_PREVIEW);
        }
      }
    }

    if (!isSneaking && data.wasSneaking) {
      data.particleLevel = Math.max(1, data.particleLevel - cfg.sneakParticleDecay());
    }

    data.wasSneaking = isSneaking;
  }

  private static void spawnFriendlyEntityHeartParticles(
      List<LivingEntity> entities, HeartParticleEffect effect) {
    for (LivingEntity entity : entities) {
      KissUtils.spawnHeartParticles(entity.level(), entity, effect);
    }
  }

  private static class SneakData {
    int count = 0;
    long lastTime = -1;
    int particleLevel = 1;
    boolean wasSneaking = false;
  }
}
