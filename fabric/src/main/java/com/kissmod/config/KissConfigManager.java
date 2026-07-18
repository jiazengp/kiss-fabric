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
  private static final Path BACKUP_DIR = CONFIG_DIR.resolve("backups");

  public static void loadConfig() {
    try {
      KissConfig config;
      if (!Files.exists(CONFIG_FILE)) {
        Constants.LOG.info("Creating default configuration file...");
        config = new KissConfig();
        saveConfig(config);
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
      return YamlConfigurations.load(CONFIG_FILE, KissConfig.class);
    } catch (Exception e) {
      Constants.LOG.error("Failed to parse configuration file: {}", e.getMessage());
      return handleCorruptedConfig(e);
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
}
