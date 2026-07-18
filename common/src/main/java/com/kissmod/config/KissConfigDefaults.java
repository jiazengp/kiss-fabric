package com.kissmod.config;

import java.util.List;

/**
 * Centralized default values for all KissMod configuration options. Both Fabric and NeoForge
 * platform configs reference these constants to ensure defaults never drift apart.
 */
public final class KissConfigDefaults {

  // ── Kiss command ──────────────────────────────────────────
  public static final boolean ENABLE_KISS_COMMAND = true;
  public static final int COMMAND_COOLDOWN_SECONDS = 10;
  public static final int MIN_COMMAND_COOLDOWN_SECONDS = 0;
  public static final int MAX_COMMAND_COOLDOWN_SECONDS = 600;
  public static final String KISS_MESSAGE = "%s kissed you!";
  public static final String KISS_PROMPT_MESSAGE = "You kissed %s!";
  public static final String SELF_KISS_ERROR_MESSAGE = "You can't kiss yourself!";
  public static final String COOLDOWN_ERROR_MESSAGE = "Wait %d seconds before kissing again!";

  // ── Sneak particle ───────────────────────────────────────
  public static final int DEFAULT_MAX_SNEAK_PARTICLES = 6;
  public static final int MIN_SNEAK_PARTICLES = 0;
  public static final int MAX_SNEAK_PARTICLES_LIMIT = 64;
  public static final int SNEAK_PARTICLE_DECAY = 2;
  public static final int MIN_SNEAK_PARTICLE_DECAY = 0;
  public static final int MAX_SNEAK_PARTICLE_DECAY = 64;
  public static final int SNEAK_TRIGGER_COUNT = 3;
  public static final int MIN_SNEAK_TRIGGER_COUNT = 1;
  public static final int MAX_SNEAK_TRIGGER_COUNT = 20;
  public static final int SNEAK_TRIGGER_WINDOW_TICKS = 40;
  public static final int MIN_SNEAK_TRIGGER_WINDOW_TICKS = 1;
  public static final int MAX_SNEAK_TRIGGER_WINDOW_TICKS = 200;
  public static final double SNEAK_PREVIEW_RADIUS = 6.0;
  public static final double MIN_SNEAK_PREVIEW_RADIUS = 0.0;
  public static final double MAX_SNEAK_PREVIEW_RADIUS = 64.0;
  public static final double SNEAK_TRIGGER_RADIUS = 16.0;
  public static final double MIN_SNEAK_TRIGGER_RADIUS = 1.0;
  public static final double MAX_SNEAK_TRIGGER_RADIUS = 64.0;
  public static final double MAX_VIEW_ANGLE = 90.0;
  public static final double MIN_VIEW_ANGLE = 0.0;
  public static final double MAX_VIEW_ANGLE_LIMIT = 180.0;

  // ── Entity particles ─────────────────────────────────────
  public static final boolean ENABLE_ENTITY_PARTICLES = true;
  public static final double ENTITY_PARTICLE_RADIUS = 8.0;
  public static final double MIN_ENTITY_PARTICLE_RADIUS = 1.0;
  public static final double MAX_ENTITY_PARTICLE_RADIUS = 32.0;
  public static final double HEART_PARTICLE_HEIGHT_OFFSET = 0.35;
  public static final double MIN_HEART_PARTICLE_HEIGHT_OFFSET = 0.0;
  public static final double MAX_HEART_PARTICLE_HEIGHT_OFFSET = 4.0;
  public static final List<String> FRIENDLY_ENTITY_TYPES =
      List.of(
          "minecraft:villager",
          "minecraft:cow",
          "minecraft:sheep",
          "minecraft:pig",
          "minecraft:chicken",
          "minecraft:horse",
          "minecraft:donkey",
          "minecraft:mule",
          "minecraft:llama",
          "minecraft:trader_llama",
          "minecraft:camel",
          "minecraft:parrot",
          "minecraft:cat",
          "minecraft:wolf",
          "minecraft:ocelot",
          "minecraft:fox",
          "minecraft:panda",
          "minecraft:bee",
          "minecraft:rabbit",
          "minecraft:turtle",
          "minecraft:mooshroom",
          "minecraft:sniffer",
          "minecraft:iron_golem",
          "minecraft:snow_golem",
          "minecraft:axolotl",
          "minecraft:dolphin");
  public static final List<String> FRIENDLY_ENTITY_TAGS = List.of();

  private KissConfigDefaults() {}
}
