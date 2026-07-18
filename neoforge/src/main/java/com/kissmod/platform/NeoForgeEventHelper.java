package com.kissmod.platform;

import com.kissmod.KissCommand;
import com.kissmod.KissModCommon;
import com.kissmod.platform.services.IKissEventHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class NeoForgeEventHelper implements IKissEventHelper {

  @Override
  public void registerServerTickHandler() {
    NeoForgeTickCompat.register(this::onServerTick);
  }

  @Override
  public void registerCommandHandler() {
    NeoForge.EVENT_BUS.addListener(
        (RegisterCommandsEvent event) -> onRegisterCommands(event.getDispatcher()));
  }

  private void onServerTick(MinecraftServer server) {
    for (ServerLevel level : server.getAllLevels()) {
      onServerTick(level);
    }
  }

  @Override
  public void onServerTick(ServerLevel level) {
    KissModCommon.onServerTick(level);
  }

  @Override
  public void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
    KissCommand.register(dispatcher);
  }
}
