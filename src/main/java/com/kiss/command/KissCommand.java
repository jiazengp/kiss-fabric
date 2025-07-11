package com.kiss.command;

import com.kiss.config.KissConfig;
import com.kiss.utils.KissUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Duration;
import java.util.UUID;

public final class KissCommand {
    private KissCommand() {} // 禁止实例化

    private static final Duration ZERO_DURATION = Duration.ZERO;
    private static final Object2LongOpenHashMap<UUID> lastKissTimestamps = new Object2LongOpenHashMap<>();
    private static final int HEART_PARTICLE_COUNT = 3;

    private static KissConfig config;

    public static void setConfig(KissConfig newConfig) {
        config = newConfig;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        if (!config.enableKissCommand) return;

        dispatcher.register(CommandManager.literal("kiss")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(KissCommand::handleKiss)
                )
        );
    }

    private static int handleKiss(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sourcePlayer = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");

        if (KissUtils.isSamePlayer(sourcePlayer, targetPlayer)) {
            KissUtils.sendError(context.getSource(), config.selfKissErrorMessage, Formatting.DARK_RED);
            return 0;
        }

        Duration remainingCooldown = checkAndUpdateCooldown(sourcePlayer.getUuid());
        if (!remainingCooldown.isZero()) {
            KissUtils.sendError(
                    context.getSource(),
                    String.format(config.cooldownErrorMessage, remainingCooldown.toSeconds()),
                    Formatting.YELLOW
            );
            return 0;
        }

        sendKiss(sourcePlayer, targetPlayer);
        return Command.SINGLE_SUCCESS;
    }

    private static Duration checkAndUpdateCooldown(UUID playerId) {
        long currentTimeMillis = System.currentTimeMillis();
        long lastKissTime = lastKissTimestamps.getOrDefault(playerId, 0L);
        long cooldownMillis = config.commandCooldown * 1000L;
        long timeSinceLastKiss = currentTimeMillis - lastKissTime;

        if (timeSinceLastKiss < cooldownMillis) {
            return Duration.ofMillis(cooldownMillis - timeSinceLastKiss);
        }

        lastKissTimestamps.put(playerId, currentTimeMillis);
        return ZERO_DURATION;
    }

    private static void sendKiss(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer) {
        Text messageToTarget = KissUtils.buildMessage(config.kissMessage, sourcePlayer.getDisplayName(), Formatting.LIGHT_PURPLE);
        Text messageToSource = KissUtils.buildMessage(config.kissPromptMessage, targetPlayer.getDisplayName(), Formatting.GRAY);

        sourcePlayer.sendMessage(messageToSource, false);
        targetPlayer.sendMessage(messageToTarget, false);

        sourcePlayer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        targetPlayer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        if (KissUtils.isPlayerInvisible(targetPlayer)) return;

        KissUtils.spawnHeartParticlesForPlayer(targetPlayer.getWorld(), targetPlayer, HEART_PARTICLE_COUNT, 0.3, 0.1);
    }
}
