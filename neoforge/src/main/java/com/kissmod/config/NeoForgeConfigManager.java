package com.kissmod.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.kissmod.Constants;
import com.kissmod.platform.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Loads NeoForge configuration and performs one-time migration from legacy TOML files. */
public final class NeoForgeConfigManager {
  private static final Path LEGACY_CONFIG_FILE =
      Services.PLATFORM.getConfigDirectory().resolve("kissmod-common.toml");

  private NeoForgeConfigManager() {}

  public static KissConfigData load() {
    KissConfigData current = NeoForgeKissConfig.toData();
    if (!Files.exists(LEGACY_CONFIG_FILE)) {
      return current;
    }

    if (!current.equals(KissConfigData.defaults())) {
      Constants.LOG.info(
          "Skipped legacy NeoForge configuration migration because the current configuration already contains custom values");
      return current;
    }

    try {
      KissConfigData migrated = LegacyKissConfigMapper.mergeWithDefaults(readLegacyValues());
      NeoForgeKissConfig.update(migrated);
      deleteLegacyConfig();
      return migrated;
    } catch (Exception e) {
      Constants.LOG.warn(
          "Failed to migrate legacy NeoForge configuration from {}; using the current configuration. The legacy file was left unchanged.",
          LEGACY_CONFIG_FILE,
          e);
      return current;
    }
  }

  private static Map<String, Object> readLegacyValues() {
    try (CommentedFileConfig config = CommentedFileConfig.builder(LEGACY_CONFIG_FILE).build()) {
      config.load();
      Map<String, Object> values = new HashMap<>();
      config.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getRawValue()));
      return Map.copyOf(values);
    }
  }

  private static void deleteLegacyConfig() {
    try {
      Files.deleteIfExists(LEGACY_CONFIG_FILE);
      Constants.LOG.info("Removed legacy NeoForge configuration {}", LEGACY_CONFIG_FILE);
    } catch (IOException e) {
      Constants.LOG.warn(
          "The current NeoForge configuration is ready, but the legacy file could not be removed: {}",
          LEGACY_CONFIG_FILE,
          e);
    }
  }
}
