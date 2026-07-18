package com.kissmod.config;

/**
 * Global access point for the resolved server configuration.
 *
 * <p>Each platform must call {@link #update(KissConfigData)} during startup (or after a config
 * reload) so that the rest of the mod can read values through {@link #server()}.
 */
public final class KissConfigs {

  private static volatile KissConfigData SERVER = KissConfigData.defaults();

  /** Set or replace the active configuration snapshot. */
  public static void update(KissConfigData data) {
    SERVER = data;
  }

  /**
   * @return the current server configuration (never null).
   */
  public static KissServerConfig server() {
    return SERVER;
  }

  private KissConfigs() {}
}
