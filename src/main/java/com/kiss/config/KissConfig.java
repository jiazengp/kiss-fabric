// KissConfig.java
package com.kiss.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kiss.LoveSneakMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class KissConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("kissmod.json");

    public String kissMessage = "%s Kissed you!";
    public int commandCooldown = 10; // in seconds
    public int maxSneakParticles = 6;
    public double sneakTriggerRadius = 5.0;

    public static KissConfig load() {
        File configFile = CONFIG_PATH.toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, KissConfig.class);
            } catch (IOException e) {
                LoveSneakMod.LOGGER.error("Failed to load config", e);
            }
        }
        return new KissConfig();
    }

    public void save() {
        File configFile = CONFIG_PATH.toFile();
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LoveSneakMod.LOGGER.error("Failed to save config", e);
        }
    }
}