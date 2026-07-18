package com.kissmod;

/** Named particle parameters shared by command and sneak-trigger effects. */
public record HeartParticleEffect(int count, double offset, double speed) {

  public static final HeartParticleEffect COMMAND = new HeartParticleEffect(3, 0.3, 0.1);
  public static final HeartParticleEffect SNEAK_PREVIEW = new HeartParticleEffect(1, 0.1, 0.05);
  public static final HeartParticleEffect FRIENDLY_TRIGGER = new HeartParticleEffect(2, 0.2, 0.1);

  public static HeartParticleEffect playerTrigger(int count) {
    return new HeartParticleEffect(count, 0.3, 0.1);
  }
}
