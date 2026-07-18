package com.kissmod.platform;

import com.kissmod.KissCommand;
import com.kissmod.KissModCommon;
import com.kissmod.platform.services.IKissEventHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;

public class FabricEventHelper implements IKissEventHelper {

  @Override
  public void registerServerTickHandler() {
    FabricTickCompat.register(this::onServerTick);
  }

  @Override
  public void registerCommandHandler() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> onRegisterCommands(dispatcher));
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
