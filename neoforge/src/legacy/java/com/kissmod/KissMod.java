package com.kissmod;

import com.kissmod.config.KissConfigs;
import com.kissmod.config.NeoForgeKissConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(Constants.MOD_ID)
public class KissMod {

  public KissMod(IEventBus modEventBus) {
    Constants.LOG.info("Hello NeoForge world from KissMod!");

    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, NeoForgeKissConfig.SPEC);
    KissModCommon.init();
    NeoForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  private void onServerAboutToStart(ServerAboutToStartEvent event) {
    KissConfigs.update(NeoForgeKissConfig.toData());
    Constants.LOG.info("[KissMod] Server config snapshot built");
  }
}
