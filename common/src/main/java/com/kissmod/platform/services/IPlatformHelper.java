package com.kissmod.platform.services;

import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;

public interface IPlatformHelper {
  /**
   * Gets the name of the current platform
   *
   * @return The name of the current platform.
   */
  String getPlatformName();

  /**
   * Checks if a mod with the given id is loaded.
   *
   * @param modId The mod to check if it is loaded.
   * @return True if the mod is loaded, false otherwise.
   */
  boolean isModLoaded(String modId);

  /**
   * Check if the game is currently in a development environment.
   *
   * @return True if in a development environment, false otherwise.
   */
  boolean isDevelopmentEnvironment();

  /** Returns whether the player is a loader-provided fake player. */
  boolean isFakePlayer(ServerPlayer player);

  /** Checks a KissMod permission path, falling back to allowing all players. */
  boolean hasPermission(ServerPlayer player, String permissionPath);

  /**
   * Gets the name of the environment type as a string.
   *
   * @return The name of the environment type.
   */
  default String getEnvironmentName() {

    return isDevelopmentEnvironment() ? "development" : "production";
  }

  Path getConfigDirectory();
}
