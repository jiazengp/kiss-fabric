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
    // These instances carry a unit codec and cannot be serialized, so they must not enter the test
    // registry of a run that players connect to. Every development launch has the GameTest system
    // active, so the run itself has to declare that it is the GameTest server.
    if (!Boolean.getBoolean("kissmod.gametestServer")) {
      return;
    }
    KissDataGameTestBootstrap.register(event);
  }
}
