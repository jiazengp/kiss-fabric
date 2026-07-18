package com.kissmod.config;

import java.util.List;

public interface KissServerConfig {

  // Kiss command
  String kissMessage();

  String kissPromptMessage();

  String selfKissErrorMessage();

  String cooldownErrorMessage();

  int commandCooldownSeconds();

  boolean enableKissCommand();

  // Sneak particle
  int maxSneakParticles();

  int sneakParticleDecay();

  int sneakTriggerCount();

  int sneakTriggerWindowTicks();

  double sneakPreviewRadius();

  double sneakTriggerRadius();

  double maxViewAngle();

  // Entity particles
  boolean enableEntityParticles();

  double entityParticleRadius();

  double heartParticleHeightOffset();

  List<String> friendlyEntityTypes();

  List<String> friendlyEntityTags();
}
