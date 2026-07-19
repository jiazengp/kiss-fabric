package com.kissmod.config;

import com.kissmod.Constants;
import com.kissmod.platform.Services;
import de.exlll.configlib.YamlConfigurations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KissConfigManager {
  private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  private static final Path CONFIG_DIR =
      Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID);
  private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.yml");
  private static final Path LEGACY_CONFIG_FILE =
      Services.PLATFORM.getConfigDirectory().resolve("kissmod.json");
  private static final Path BACKUP_DIR = CONFIG_DIR.resolve("backups");

  public static void loadConfig() {
    try {
      KissConfig config;
      if (!Files.exists(CONFIG_FILE)) {
        InitialConfig initial = loadLegacyConfigOrDefaults();
        config = initial.config();
        saveConfig(config);
        if (initial.migrated()) {
          deleteLegacyConfig();
        }
      } else {
        config = loadConfigWithValidation();
      }
      KissConfigs.update(toData(config));
    } catch (Exception e) {
      Constants.LOG.error("Failed to load configuration", e);
      throw new RuntimeException("Failed to load or create config file", e);
    }
  }

  private static KissConfigData toData(KissConfig c) {
    return new KissConfigData(
        c.enableKissCommand,
        c.commandCooldownSeconds,
        c.kissMessage,
        c.kissPromptMessage,
        c.selfKissErrorMessage,
        c.cooldownErrorMessage,
        c.maxSneakParticles,
        c.sneakParticleDecay,
        c.sneakTriggerCount,
        c.sneakTriggerWindowTicks,
        c.sneakPreviewRadius,
        c.sneakTriggerRadius,
        c.maxViewAngle,
        c.enableEntityParticles,
        c.entityParticleRadius,
        c.heartParticleHeightOffset,
        c.friendlyEntityTypes,
        c.friendlyEntityTags);
  }

  private static KissConfig loadConfigWithValidation() throws IOException {
    try {
      // Preserve existing values and rewrite the file with defaults for newly added fields.
      return YamlConfigurations.update(CONFIG_FILE, KissConfig.class);
    } catch (Exception e) {
      Constants.LOG.error("Failed to parse configuration file: {}", e.getMessage());
      return handleCorruptedConfig(e);
    }
  }

  private static InitialConfig loadLegacyConfigOrDefaults() {
    if (!Files.exists(LEGACY_CONFIG_FILE)) {
      Constants.LOG.info("Creating default configuration file...");
      return new InitialConfig(new KissConfig(), false);
    }
    try {
      KissConfig migrated = KissConfig.fromData(FabricLegacyConfigReader.load(LEGACY_CONFIG_FILE));
      return new InitialConfig(migrated, true);
    } catch (Exception e) {
      Constants.LOG.warn(
          "Failed to migrate legacy configuration from {}; using defaults. The legacy file was left unchanged.",
          LEGACY_CONFIG_FILE,
          e);
      return new InitialConfig(new KissConfig(), false);
    }
  }

  private static void deleteLegacyConfig() {
    try {
      Files.deleteIfExists(LEGACY_CONFIG_FILE);
      Constants.LOG.info(
          "Migrated legacy configuration to {} and removed {}", CONFIG_FILE, LEGACY_CONFIG_FILE);
    } catch (IOException e) {
      Constants.LOG.warn(
          "Configuration migration succeeded, but the legacy file could not be removed: {}",
          LEGACY_CONFIG_FILE,
          e);
    }
  }

  private static KissConfig handleCorruptedConfig(Exception originalException) throws IOException {
    String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
    Path backupPath = BACKUP_DIR.resolve("config_corrupted_" + timestamp + ".yml");

    try {
      Files.createDirectories(BACKUP_DIR);
      Files.copy(CONFIG_FILE, backupPath, StandardCopyOption.REPLACE_EXISTING);
      Constants.LOG.info("Backed up corrupted configuration to: {}", backupPath);
    } catch (IOException e) {
      Constants.LOG.error("Failed to backup corrupted configuration", e);
    }

    return resetToDefault("Configuration file is corrupted: " + originalException.getMessage());
  }

  private static KissConfig resetToDefault(String reason) {
    Constants.LOG.warn("Resetting configuration to defaults. Reason: {}", reason);

    KissConfig defaultConfig = new KissConfig();
    try {
      Files.createDirectories(CONFIG_DIR);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create configuration directory", e);
    }
    YamlConfigurations.save(CONFIG_FILE, KissConfig.class, defaultConfig);

    Constants.LOG.info("Configuration has been reset to defaults and saved");
    return defaultConfig;
  }

  private static void saveConfig(KissConfig config) {
    try {
      Files.createDirectories(CONFIG_DIR);
      YamlConfigurations.save(CONFIG_FILE, KissConfig.class, config);
      Constants.LOG.debug("Configuration saved successfully");
    } catch (Exception e) {
      Constants.LOG.error("Failed to save configuration", e);
      throw new RuntimeException("Failed to save configuration", e);
    }
  }

  private record InitialConfig(KissConfig config, boolean migrated) {}
}
