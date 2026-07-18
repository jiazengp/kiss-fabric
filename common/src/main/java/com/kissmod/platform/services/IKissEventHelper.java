package com.kissmod.platform.services;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;

public interface IKissEventHelper {

  /** Registers server tick event handler */
  void registerServerTickHandler();

  /** Registers command registration handler */
  void registerCommandHandler();

  /**
   * Called when server tick occurs for a level
   *
   * @param level The server level that ticked
   */
  void onServerTick(ServerLevel level);

  /**
   * Called when commands should be registered
   *
   * @param dispatcher The command dispatcher
   */
  void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher);
}
