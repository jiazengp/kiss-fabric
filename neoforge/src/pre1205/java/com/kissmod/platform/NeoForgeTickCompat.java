package com.kissmod.platform;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

final class NeoForgeTickCompat {

  private NeoForgeTickCompat() {}

  static void register(Consumer<MinecraftServer> handler) {
    NeoForge.EVENT_BUS.addListener(
        (TickEvent.ServerTickEvent event) -> {
          if (event.phase == TickEvent.Phase.END) {
            handler.accept(event.getServer());
          }
        });
  }
}
