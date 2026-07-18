package com.kissmod.config;

import java.util.List;

/**
 * Immutable snapshot of all KissMod configuration values. Implements {@link KissServerConfig}
 * directly — the record accessor methods satisfy the interface contract without any delegation
 * boilerplate.
 *
 * <p>Each platform is responsible for building a {@code KissConfigData} from its native config
 * format (YAML/JSON on Fabric, TOML on NeoForge) and calling {@link
 * KissConfigs#update(KissConfigData)}.
 */
public record KissConfigData(
    // ── Kiss command ──────────────────────────────────────────
    boolean enableKissCommand,
    int commandCooldownSeconds,
    String kissMessage,
    String kissPromptMessage,
    String selfKissErrorMessage,
    String cooldownErrorMessage,

    // ── Sneak particle ───────────────────────────────────────
    int maxSneakParticles,
    int sneakParticleDecay,
    int sneakTriggerCount,
    int sneakTriggerWindowTicks,
    double sneakPreviewRadius,
    double sneakTriggerRadius,
    double maxViewAngle,

    // ── Entity particles ─────────────────────────────────────
    boolean enableEntityParticles,
    double entityParticleRadius,
    double heartParticleHeightOffset,
    List<String> friendlyEntityTypes,
    List<String> friendlyEntityTags)
    implements KissServerConfig {

  public KissConfigData {
    commandCooldownSeconds =
        clamp(
            commandCooldownSeconds,
            KissConfigDefaults.MIN_COMMAND_COOLDOWN_SECONDS,
            KissConfigDefaults.MAX_COMMAND_COOLDOWN_SECONDS);
    kissMessage = valueOrDefault(kissMessage, KissConfigDefaults.KISS_MESSAGE);
    kissPromptMessage = valueOrDefault(kissPromptMessage, KissConfigDefaults.KISS_PROMPT_MESSAGE);
    selfKissErrorMessage =
        valueOrDefault(selfKissErrorMessage, KissConfigDefaults.SELF_KISS_ERROR_MESSAGE);
    cooldownErrorMessage =
        valueOrDefault(cooldownErrorMessage, KissConfigDefaults.COOLDOWN_ERROR_MESSAGE);
    maxSneakParticles =
        clamp(
            maxSneakParticles,
            KissConfigDefaults.MIN_SNEAK_PARTICLES,
            KissConfigDefaults.MAX_SNEAK_PARTICLES_LIMIT);
    sneakParticleDecay =
        clamp(
            sneakParticleDecay,
            KissConfigDefaults.MIN_SNEAK_PARTICLE_DECAY,
            KissConfigDefaults.MAX_SNEAK_PARTICLE_DECAY);
    sneakTriggerCount =
        clamp(
            sneakTriggerCount,
            KissConfigDefaults.MIN_SNEAK_TRIGGER_COUNT,
            KissConfigDefaults.MAX_SNEAK_TRIGGER_COUNT);
    sneakTriggerWindowTicks =
        clamp(
            sneakTriggerWindowTicks,
            KissConfigDefaults.MIN_SNEAK_TRIGGER_WINDOW_TICKS,
            KissConfigDefaults.MAX_SNEAK_TRIGGER_WINDOW_TICKS);
    sneakPreviewRadius =
        clamp(
            sneakPreviewRadius,
            KissConfigDefaults.MIN_SNEAK_PREVIEW_RADIUS,
            KissConfigDefaults.MAX_SNEAK_PREVIEW_RADIUS,
            KissConfigDefaults.SNEAK_PREVIEW_RADIUS);
    sneakTriggerRadius =
        clamp(
            sneakTriggerRadius,
            KissConfigDefaults.MIN_SNEAK_TRIGGER_RADIUS,
            KissConfigDefaults.MAX_SNEAK_TRIGGER_RADIUS,
            KissConfigDefaults.SNEAK_TRIGGER_RADIUS);
    maxViewAngle =
        clamp(
            maxViewAngle,
            KissConfigDefaults.MIN_VIEW_ANGLE,
            KissConfigDefaults.MAX_VIEW_ANGLE_LIMIT,
            KissConfigDefaults.MAX_VIEW_ANGLE);
    entityParticleRadius =
        clamp(
            entityParticleRadius,
            KissConfigDefaults.MIN_ENTITY_PARTICLE_RADIUS,
            KissConfigDefaults.MAX_ENTITY_PARTICLE_RADIUS,
            KissConfigDefaults.ENTITY_PARTICLE_RADIUS);
    heartParticleHeightOffset =
        clamp(
            heartParticleHeightOffset,
            KissConfigDefaults.MIN_HEART_PARTICLE_HEIGHT_OFFSET,
            KissConfigDefaults.MAX_HEART_PARTICLE_HEIGHT_OFFSET,
            KissConfigDefaults.HEART_PARTICLE_HEIGHT_OFFSET);
    friendlyEntityTypes =
        copyEntries(friendlyEntityTypes, KissConfigDefaults.FRIENDLY_ENTITY_TYPES);
    friendlyEntityTags = copyEntries(friendlyEntityTags, KissConfigDefaults.FRIENDLY_ENTITY_TAGS);
  }

  private static int clamp(int value, int minimum, int maximum) {
    return Math.max(minimum, Math.min(maximum, value));
  }

  private static double clamp(double value, double minimum, double maximum, double defaultValue) {
    if (!Double.isFinite(value)) {
      return defaultValue;
    }
    return Math.max(minimum, Math.min(maximum, value));
  }

  private static String valueOrDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  private static List<String> copyEntries(List<String> entries, List<String> defaults) {
    if (entries == null) {
      return defaults;
    }
    return entries.stream()
        .filter(entry -> entry != null && !entry.isBlank())
        .map(String::trim)
        .distinct()
        .toList();
  }

  @Override
  public List<String> friendlyEntityTypes() {
    return List.copyOf(friendlyEntityTypes);
  }

  @Override
  public List<String> friendlyEntityTags() {
    return List.copyOf(friendlyEntityTags);
  }

  /** Convenience factory using {@link KissConfigDefaults}. */
  public static KissConfigData defaults() {
    return new KissConfigData(
        KissConfigDefaults.ENABLE_KISS_COMMAND,
        KissConfigDefaults.COMMAND_COOLDOWN_SECONDS,
        KissConfigDefaults.KISS_MESSAGE,
        KissConfigDefaults.KISS_PROMPT_MESSAGE,
        KissConfigDefaults.SELF_KISS_ERROR_MESSAGE,
        KissConfigDefaults.COOLDOWN_ERROR_MESSAGE,
        KissConfigDefaults.DEFAULT_MAX_SNEAK_PARTICLES,
        KissConfigDefaults.SNEAK_PARTICLE_DECAY,
        KissConfigDefaults.SNEAK_TRIGGER_COUNT,
        KissConfigDefaults.SNEAK_TRIGGER_WINDOW_TICKS,
        KissConfigDefaults.SNEAK_PREVIEW_RADIUS,
        KissConfigDefaults.SNEAK_TRIGGER_RADIUS,
        KissConfigDefaults.MAX_VIEW_ANGLE,
        KissConfigDefaults.ENABLE_ENTITY_PARTICLES,
        KissConfigDefaults.ENTITY_PARTICLE_RADIUS,
        KissConfigDefaults.HEART_PARTICLE_HEIGHT_OFFSET,
        KissConfigDefaults.FRIENDLY_ENTITY_TYPES,
        KissConfigDefaults.FRIENDLY_ENTITY_TAGS);
  }
}
