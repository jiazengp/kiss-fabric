package com.kissmod.config;

import java.util.Map;

/** Maps configuration keys used by KissMod v1 releases to the current shared snapshot. */
public final class LegacyKissConfigMapper {

  private LegacyKissConfigMapper() {}

  public static KissConfigData mergeWithDefaults(Map<?, ?> values) {
    KissConfigData defaults = KissConfigData.defaults();
    return new KissConfigData(
        booleanValue(values, "enableKissCommand", defaults.enableKissCommand()),
        intValue(values, "commandCooldown", defaults.commandCooldownSeconds()),
        stringValue(values, "kissMessage", defaults.kissMessage()),
        stringValue(values, "kissPromptMessage", defaults.kissPromptMessage()),
        stringValue(values, "selfKissErrorMessage", defaults.selfKissErrorMessage()),
        stringValue(values, "cooldownErrorMessage", defaults.cooldownErrorMessage()),
        intValue(values, "maxSneakParticles", defaults.maxSneakParticles()),
        intValue(values, "sneakParticleDecay", defaults.sneakParticleDecay()),
        intValue(values, "sneakTriggerCount", defaults.sneakTriggerCount()),
        intValue(values, "sneakTimeWindow", defaults.sneakTriggerWindowTicks()),
        defaults.sneakPreviewRadius(),
        doubleValue(values, "sneakTriggerRadius", defaults.sneakTriggerRadius()),
        doubleValue(values, "maxViewAngleDegree", defaults.maxViewAngle()),
        defaults.enableEntityParticles(),
        doubleValue(values, "mobTriggerRadius", defaults.entityParticleRadius()),
        defaults.heartParticleHeightOffset(),
        defaults.friendlyEntityTypes(),
        defaults.friendlyEntityTags());
  }

  private static boolean booleanValue(Map<?, ?> values, String name, boolean defaultValue) {
    Object value = values.get(name);
    return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
  }

  private static int intValue(Map<?, ?> values, String name, int defaultValue) {
    Object value = values.get(name);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }

  private static double doubleValue(Map<?, ?> values, String name, double defaultValue) {
    Object value = values.get(name);
    return value instanceof Number number ? number.doubleValue() : defaultValue;
  }

  private static String stringValue(Map<?, ?> values, String name, String defaultValue) {
    Object value = values.get(name);
    return value instanceof String stringValue ? stringValue : defaultValue;
  }
}
