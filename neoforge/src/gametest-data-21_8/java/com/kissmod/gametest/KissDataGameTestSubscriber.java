package com.kissmod.gametest;

import com.kissmod.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public final class KissDataGameTestSubscriber {
  private KissDataGameTestSubscriber() {}

  @SubscribeEvent
  public static void register(RegisterGameTestsEvent event) {
    KissDataGameTestBootstrap.register(event);
  }
}
