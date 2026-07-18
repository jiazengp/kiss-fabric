package com.kissmod.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class KissConfig {

  // Kiss command settings
  @Comment("Kiss command feedback message for")
  public String kissMessage = KissConfigDefaults.KISS_MESSAGE;

  public String kissPromptMessage = KissConfigDefaults.KISS_PROMPT_MESSAGE;
  public String selfKissErrorMessage = KissConfigDefaults.SELF_KISS_ERROR_MESSAGE;
  public String cooldownErrorMessage = KissConfigDefaults.COOLDOWN_ERROR_MESSAGE;
  public int commandCooldownSeconds = KissConfigDefaults.COMMAND_COOLDOWN_SECONDS;
  public boolean enableKissCommand = KissConfigDefaults.ENABLE_KISS_COMMAND;

  // Sneak particle settings
  public int maxSneakParticles = KissConfigDefaults.DEFAULT_MAX_SNEAK_PARTICLES;
  public int sneakParticleDecay = KissConfigDefaults.SNEAK_PARTICLE_DECAY;
  public int sneakTriggerCount = KissConfigDefaults.SNEAK_TRIGGER_COUNT;
  public int sneakTriggerWindowTicks = KissConfigDefaults.SNEAK_TRIGGER_WINDOW_TICKS;
  public double sneakPreviewRadius = KissConfigDefaults.SNEAK_PREVIEW_RADIUS;
  public double sneakTriggerRadius = KissConfigDefaults.SNEAK_TRIGGER_RADIUS;
  public double maxViewAngle = KissConfigDefaults.MAX_VIEW_ANGLE;

  // Entity particle settings for sneak triggering
  public boolean enableEntityParticles = KissConfigDefaults.ENABLE_ENTITY_PARTICLES;
  public double entityParticleRadius = KissConfigDefaults.ENTITY_PARTICLE_RADIUS;
  public double heartParticleHeightOffset = KissConfigDefaults.HEART_PARTICLE_HEIGHT_OFFSET;
  public List<String> friendlyEntityTypes =
      new ArrayList<>(KissConfigDefaults.FRIENDLY_ENTITY_TYPES);
  public List<String> friendlyEntityTags = new ArrayList<>(KissConfigDefaults.FRIENDLY_ENTITY_TAGS);
}
