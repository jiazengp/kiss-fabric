package com.kiss.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kiss.LoveSneakMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class KissConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("kissmod.json");

    public int commandCooldown = 10; // seconds
    public int sneakTriggerCount = 3;
    public int sneakTimeWindow = 40;
    public int sneakParticleDecay = 2;
    public int maxSneakParticles = 6;
    public int mobTriggerRadius = 2;
    public int sneakLongThreshold = 40;

    public double sneakTriggerRadius = 12.0;
    public double maxViewAngleDegree = 90.0;

    public boolean enableKissCommand = true;


    public static KissConfig load() {
        File configFile = CONFIG_PATH.toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, KissConfig.class);
            } catch (Exception e) {
                LoveSneakMod.LOGGER.error("Failed to load config, using defaults", e);
            }
        } else {
            LoveSneakMod.LOGGER.info("Config file not found, generating default");
        }

        return new KissConfig();
    }

    public void save() {
        File configFile = CONFIG_PATH.toFile();

        try {
            createParentDirsIfNeeded(configFile);

            try (Writer writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LoveSneakMod.LOGGER.error("Failed to save config", e);
        }
    }

    private void createParentDirsIfNeeded(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create parent config directory: " + parent);
        }
    }
}
