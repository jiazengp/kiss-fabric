package com.kissmod.platform;

import net.neoforged.fml.loading.FMLLoader;

final class NeoForgeLoaderCompat {

  private NeoForgeLoaderCompat() {}

  static boolean isDevelopmentEnvironment() {
    return !FMLLoader.getCurrent().isProduction();
  }
}
