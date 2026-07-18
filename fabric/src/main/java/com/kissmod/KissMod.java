package com.kissmod;

import com.kissmod.config.KissConfigManager;
import net.fabricmc.api.ModInitializer;

public class KissMod implements ModInitializer {

  @Override
  public void onInitialize() {
    Constants.LOG.info("Hello Fabric world from KissMod!");

    KissConfigManager.loadConfig();
    KissModCommon.init();
  }
}
