package com.kissmod.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeKissConfig {

  public static final ModConfigSpec SPEC;

  // Kiss command
  public static final ModConfigSpec.BooleanValue ENABLE_KISS_COMMAND;
  public static final ModConfigSpec.IntValue COMMAND_COOLDOWN_SECONDS;
  public static final ModConfigSpec.ConfigValue<String> KISS_MESSAGE;
  public static final ModConfigSpec.ConfigValue<String> KISS_PROMPT_MESSAGE;
  public static final ModConfigSpec.ConfigValue<String> SELF_KISS_ERROR_MESSAGE;
  public static final ModConfigSpec.ConfigValue<String> COOLDOWN_ERROR_MESSAGE;

  // Sneak particle
  public static final ModConfigSpec.IntValue MAX_SNEAK_PARTICLES;
  public static final ModConfigSpec.IntValue SNEAK_PARTICLE_DECAY;
  public static final ModConfigSpec.IntValue SNEAK_TRIGGER_COUNT;
  public static final ModConfigSpec.IntValue SNEAK_TRIGGER_WINDOW_TICKS;
  public static final ModConfigSpec.DoubleValue SNEAK_PREVIEW_RADIUS;
  public static final ModConfigSpec.DoubleValue SNEAK_TRIGGER_RADIUS;
  public static final ModConfigSpec.DoubleValue MAX_VIEW_ANGLE;

  // Entity particles
  public static final ModConfigSpec.BooleanValue ENABLE_ENTITY_PARTICLES;
  public static final ModConfigSpec.DoubleValue ENTITY_PARTICLE_RADIUS;
  public static final ModConfigSpec.DoubleValue HEART_PARTICLE_HEIGHT_OFFSET;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> FRIENDLY_ENTITY_TYPES;
  public static final ModConfigSpec.ConfigValue<List<? extends String>> FRIENDLY_ENTITY_TAGS;

  static {
    ModConfigSpec.Builder b = new ModConfigSpec.Builder();

    b.push("kiss");
    ENABLE_KISS_COMMAND = b.define("enableKissCommand", KissConfigDefaults.ENABLE_KISS_COMMAND);
    COMMAND_COOLDOWN_SECONDS =
        b.defineInRange(
            "commandCooldownSeconds",
            KissConfigDefaults.COMMAND_COOLDOWN_SECONDS,
            KissConfigDefaults.MIN_COMMAND_COOLDOWN_SECONDS,
            KissConfigDefaults.MAX_COMMAND_COOLDOWN_SECONDS);
    KISS_MESSAGE = b.define("kissMessage", KissConfigDefaults.KISS_MESSAGE);
    KISS_PROMPT_MESSAGE = b.define("kissPromptMessage", KissConfigDefaults.KISS_PROMPT_MESSAGE);
    SELF_KISS_ERROR_MESSAGE =
        b.define("selfKissErrorMessage", KissConfigDefaults.SELF_KISS_ERROR_MESSAGE);
    COOLDOWN_ERROR_MESSAGE =
        b.define("cooldownErrorMessage", KissConfigDefaults.COOLDOWN_ERROR_MESSAGE);
    b.pop();

    b.push("sneak");
    MAX_SNEAK_PARTICLES =
        b.defineInRange(
            "maxSneakParticles",
            KissConfigDefaults.DEFAULT_MAX_SNEAK_PARTICLES,
            KissConfigDefaults.MIN_SNEAK_PARTICLES,
            KissConfigDefaults.MAX_SNEAK_PARTICLES_LIMIT);
    SNEAK_PARTICLE_DECAY =
        b.defineInRange(
            "sneakParticleDecay",
            KissConfigDefaults.SNEAK_PARTICLE_DECAY,
            KissConfigDefaults.MIN_SNEAK_PARTICLE_DECAY,
            KissConfigDefaults.MAX_SNEAK_PARTICLE_DECAY);
    SNEAK_TRIGGER_COUNT =
        b.defineInRange(
            "sneakTriggerCount",
            KissConfigDefaults.SNEAK_TRIGGER_COUNT,
            KissConfigDefaults.MIN_SNEAK_TRIGGER_COUNT,
            KissConfigDefaults.MAX_SNEAK_TRIGGER_COUNT);
    SNEAK_TRIGGER_WINDOW_TICKS =
        b.defineInRange(
            "sneakTriggerWindowTicks",
            KissConfigDefaults.SNEAK_TRIGGER_WINDOW_TICKS,
            KissConfigDefaults.MIN_SNEAK_TRIGGER_WINDOW_TICKS,
            KissConfigDefaults.MAX_SNEAK_TRIGGER_WINDOW_TICKS);
    SNEAK_PREVIEW_RADIUS =
        b.defineInRange(
            "sneakPreviewRadius",
            KissConfigDefaults.SNEAK_PREVIEW_RADIUS,
            KissConfigDefaults.MIN_SNEAK_PREVIEW_RADIUS,
            KissConfigDefaults.MAX_SNEAK_PREVIEW_RADIUS);
    SNEAK_TRIGGER_RADIUS =
        b.defineInRange(
            "sneakTriggerRadius",
            KissConfigDefaults.SNEAK_TRIGGER_RADIUS,
            KissConfigDefaults.MIN_SNEAK_TRIGGER_RADIUS,
            KissConfigDefaults.MAX_SNEAK_TRIGGER_RADIUS);
    MAX_VIEW_ANGLE =
        b.defineInRange(
            "maxViewAngle",
            KissConfigDefaults.MAX_VIEW_ANGLE,
            KissConfigDefaults.MIN_VIEW_ANGLE,
            KissConfigDefaults.MAX_VIEW_ANGLE_LIMIT);
    b.pop();

    b.push("entityParticles");
    ENABLE_ENTITY_PARTICLES =
        b.define("enableEntityParticles", KissConfigDefaults.ENABLE_ENTITY_PARTICLES);
    ENTITY_PARTICLE_RADIUS =
        b.defineInRange(
            "entityParticleRadius",
            KissConfigDefaults.ENTITY_PARTICLE_RADIUS,
            KissConfigDefaults.MIN_ENTITY_PARTICLE_RADIUS,
            KissConfigDefaults.MAX_ENTITY_PARTICLE_RADIUS);
    HEART_PARTICLE_HEIGHT_OFFSET =
        b.defineInRange(
            "heartParticleHeightOffset",
            KissConfigDefaults.HEART_PARTICLE_HEIGHT_OFFSET,
            KissConfigDefaults.MIN_HEART_PARTICLE_HEIGHT_OFFSET,
            KissConfigDefaults.MAX_HEART_PARTICLE_HEIGHT_OFFSET);
    FRIENDLY_ENTITY_TYPES =
        b.defineListAllowEmpty(
            "friendlyEntityTypes",
            () -> KissConfigDefaults.FRIENDLY_ENTITY_TYPES,
            value -> value instanceof String);
    FRIENDLY_ENTITY_TAGS =
        b.defineListAllowEmpty(
            "friendlyEntityTags",
            () -> KissConfigDefaults.FRIENDLY_ENTITY_TAGS,
            value -> value instanceof String);
    b.pop();

    SPEC = b.build();
  }

  private NeoForgeKissConfig() {}

  /** Build a snapshot from the current NeoForge config values. */
  public static KissConfigData toData() {
    return new KissConfigData(
        ENABLE_KISS_COMMAND.get(),
        COMMAND_COOLDOWN_SECONDS.get(),
        KISS_MESSAGE.get(),
        KISS_PROMPT_MESSAGE.get(),
        SELF_KISS_ERROR_MESSAGE.get(),
        COOLDOWN_ERROR_MESSAGE.get(),
        MAX_SNEAK_PARTICLES.get(),
        SNEAK_PARTICLE_DECAY.get(),
        SNEAK_TRIGGER_COUNT.get(),
        SNEAK_TRIGGER_WINDOW_TICKS.get(),
        SNEAK_PREVIEW_RADIUS.get(),
        SNEAK_TRIGGER_RADIUS.get(),
        MAX_VIEW_ANGLE.get(),
        ENABLE_ENTITY_PARTICLES.get(),
        ENTITY_PARTICLE_RADIUS.get(),
        HEART_PARTICLE_HEIGHT_OFFSET.get(),
        List.copyOf(FRIENDLY_ENTITY_TYPES.get()),
        List.copyOf(FRIENDLY_ENTITY_TAGS.get()));
  }
}
