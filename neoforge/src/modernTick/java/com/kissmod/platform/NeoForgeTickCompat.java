package com.kissmod.platform;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class NeoForgeTickCompat {

  private NeoForgeTickCompat() {}

  static void register(Consumer<MinecraftServer> handler) {
    NeoForge.EVENT_BUS.addListener(
        (ServerTickEvent.Post event) -> handler.accept(event.getServer()));
  }
}
