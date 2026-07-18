package com.kissmod;

import com.kissmod.config.KissConfigs;
import com.kissmod.config.KissServerConfig;
import com.kissmod.platform.Services;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public class KissCommand {
  private static final Map<UUID, Long> lastKissTimes = new HashMap<>();

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("kiss")
            .requires(
                source -> {
                  ServerPlayer player = source.getPlayer();
                  return KissConfigs.server().enableKissCommand()
                      && player != null
                      && Services.PLATFORM.hasPermission(player, "command");
                })
            .then(
                Commands.argument("player", EntityArgument.player())
                    .suggests(
                        (context, builder) -> {
                          ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
                          return SharedSuggestionProvider.suggest(
                              context.getSource().getServer().getPlayerList().getPlayers().stream()
                                  .filter(player -> !KissUtils.isSamePlayer(sourcePlayer, player))
                                  .map(ServerPlayer::getScoreboardName),
                              builder);
                        })
                    .executes(KissCommand::executeKiss)));
  }

  private static int executeKiss(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    KissServerConfig cfg = KissConfigs.server();
    CommandSourceStack source = context.getSource();
    ServerPlayer sourcePlayer = source.getPlayerOrException();
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

    if (KissUtils.isSamePlayer(sourcePlayer, targetPlayer)) {
      KissUtils.sendError(source, cfg.selfKissErrorMessage(), KissUtils.DARK_RED);
      return 0;
    }

    if (!checkCooldown(sourcePlayer, cfg)) {
      long remaining = getRemainingCooldownSeconds(sourcePlayer, cfg);
      KissUtils.sendError(
          source, String.format(cfg.cooldownErrorMessage(), remaining), KissUtils.YELLOW);
      return 0;
    }

    sendKiss(sourcePlayer, targetPlayer, cfg);
    updateCooldown(sourcePlayer);

    return Command.SINGLE_SUCCESS;
  }

  private static void sendKiss(ServerPlayer source, ServerPlayer target, KissServerConfig cfg) {
    Component msgToTarget =
        KissUtils.buildMessage(cfg.kissMessage(), source.getDisplayName(), KissUtils.LIGHT_PURPLE);
    Component msgToSource =
        KissUtils.buildMessage(cfg.kissPromptMessage(), target.getDisplayName(), KissUtils.GRAY);

    source.sendSystemMessage(msgToSource);
    target.sendSystemMessage(msgToTarget);

    source.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    target.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

    if (!KissUtils.isExcludedPlayer(target)) {
      KissUtils.spawnHeartParticles(target.level(), target, HeartParticleEffect.COMMAND);
    }
  }

  private static boolean checkCooldown(ServerPlayer player, KissServerConfig cfg) {
    long now = System.currentTimeMillis();
    long lastTime = lastKissTimes.getOrDefault(player.getUUID(), 0L);
    return now - lastTime >= cfg.commandCooldownSeconds() * 1000L;
  }

  private static long getRemainingCooldownSeconds(ServerPlayer player, KissServerConfig cfg) {
    long now = System.currentTimeMillis();
    long lastTime = lastKissTimes.getOrDefault(player.getUUID(), 0L);
    long remaining = cfg.commandCooldownSeconds() * 1000L - (now - lastTime);
    return Math.max(0, (remaining + 999L) / 1000L);
  }

  private static void updateCooldown(ServerPlayer player) {
    lastKissTimes.put(player.getUUID(), System.currentTimeMillis());
  }

  static void removeOfflinePlayerState(MinecraftServer server) {
    lastKissTimes.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
  }
}
