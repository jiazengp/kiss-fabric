package com.kissmod.platform;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;

final class FabricTickCompat {

  private FabricTickCompat() {}

  static void register(Consumer<ServerLevel> handler) {
    ServerTickEvents.END_LEVEL_TICK.register(handler::accept);
  }
}
