package com.kissmod;

import com.kissmod.config.KissConfigs;
import com.kissmod.config.KissServerConfig;
import com.kissmod.platform.EntityTagCompat;
import com.kissmod.platform.Services;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class KissUtils {
  public static final ChatFormatting DARK_RED = ChatFormatting.DARK_RED;
  public static final ChatFormatting YELLOW = ChatFormatting.YELLOW;
  public static final ChatFormatting LIGHT_PURPLE = ChatFormatting.LIGHT_PURPLE;
  public static final ChatFormatting GRAY = ChatFormatting.GRAY;

  public static boolean isSamePlayer(ServerPlayer p1, ServerPlayer p2) {
    return p1.getUUID().equals(p2.getUUID());
  }

  public static boolean hasVisibleNearbyPlayers(
      ServerPlayer player, ServerLevel level, double radius, double maxViewAngle) {
    if (isExcludedPlayer(player) || radius <= 0 || maxViewAngle <= 0) {
      return false;
    }
    UUID self = player.getUUID();
    Vec3 pos = player.position();
    AABB area = new AABB(pos.add(-radius, -6, -radius), pos.add(radius, 6, radius));

    for (ServerPlayer other : level.players()) {
      if (!other.getUUID().equals(self)
          && !isExcludedPlayer(other)
          && area.contains(other.position())
          && isPlayerInViewAndCanSee(player, other, maxViewAngle)) {
        return true;
      }
    }
    return false;
  }

  public static List<LivingEntity> getFriendlyEntitiesNearby(
      ServerPlayer player, ServerLevel level, double radius) {
    KissServerConfig cfg = KissConfigs.server();
    if (!cfg.enableEntityParticles() || radius <= 0) {
      return List.of();
    }

    Vec3 pos = player.position();
    AABB area = new AABB(pos.add(-radius, -3, -radius), pos.add(radius, 3, radius));

    return level.getEntitiesOfClass(LivingEntity.class, area).stream()
        .filter(entity -> !(entity instanceof ServerPlayer) && isFriendlyEntity(entity))
        .toList();
  }

  /**
   * Returns whether a player must be ignored by automatic particle behavior.
   *
   * <p>Spectators, invisible players, and loader-provided fake players are not real visible
   * participants in the nearby-player interaction.
   */
  public static boolean isExcludedPlayer(ServerPlayer player) {
    return player == null
        || player.isSpectator()
        || player.isInvisible()
        || Services.PLATFORM.isFakePlayer(player);
  }

  public static boolean isFriendlyEntity(LivingEntity entity) {
    KissServerConfig cfg = KissConfigs.server();
    String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    if (cfg.friendlyEntityTypes().contains(entityId)) {
      return true;
    }
    for (String tagId : cfg.friendlyEntityTags()) {
      if (EntityTagCompat.is(entity.getType(), tagId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isPlayerInViewAndCanSee(
      ServerPlayer observer, ServerPlayer target, double maxViewAngle) {
    if (!Objects.equals(observer.level(), target.level())) return false;

    Vec3 lookVec = observer.getLookAngle().normalize();
    Vec3 dirToTarget = target.position().subtract(observer.position()).normalize();

    double dot = Math.max(-1.0, Math.min(1.0, lookVec.dot(dirToTarget)));
    double angleRad = Math.acos(dot);
    double maxAngleRad = Math.toRadians(maxViewAngle);

    return angleRad <= maxAngleRad;
  }

  public static void spawnHeartParticles(
      ServerLevel level, ServerPlayer player, HeartParticleEffect effect) {
    Vec3 lookVec = player.getLookAngle().multiply(0.3, 0.3, 0.3);
    Vec3 pos = player.position();
    double x = pos.x + lookVec.x;
    double y = heartParticleY(player);
    double z = pos.z + lookVec.z;
    level.sendParticles(
        ParticleTypes.HEART,
        x,
        y,
        z,
        effect.count(),
        effect.offset(),
        effect.offset(),
        effect.offset(),
        effect.speed());
  }

  public static void spawnHeartParticles(
      Level level, LivingEntity entity, HeartParticleEffect effect) {
    Vec3 pos = entity.position();
    double x = pos.x;
    double y = heartParticleY(entity);
    double z = pos.z;

    if (level instanceof ServerLevel serverLevel) {
      serverLevel.sendParticles(
          ParticleTypes.HEART,
          x,
          y,
          z,
          effect.count(),
          effect.offset(),
          effect.offset(),
          effect.offset(),
          effect.speed());
    }
  }

  private static double heartParticleY(LivingEntity entity) {
    return entity.position().y
        + entity.getBbHeight()
        + KissConfigs.server().heartParticleHeightOffset();
  }

  public static void sendError(CommandSourceStack source, String message, ChatFormatting color) {
    source.sendFailure(Component.literal(message).withStyle(color));
  }

  public static MutableComponent buildMessage(
      String template, Component playerName, ChatFormatting color) {
    try {
      String[] parts = template.split("%s", -1);
      MutableComponent result = Component.literal(parts[0]);
      for (int i = 1; i < parts.length; i++) {
        result = result.append(playerName).append(Component.literal(parts[i]));
      }
      return result.withStyle(color);
    } catch (Exception e) {
      return Component.literal(playerName.getString()).withStyle(color);
    }
  }
}
