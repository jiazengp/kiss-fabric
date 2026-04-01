package com.kiss.command;

import com.kiss.config.KissConfig;
import com.kiss.utils.KissUtils;
import com.kiss.utils.PermissionChecker;
import com.kiss.utils.TextUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registryAccess,
                                Commands.CommandSelection environment) {
        if (!config.enableKissCommand) return;

        dispatcher.register(Commands.literal("kiss")
                .requires(ctx -> PermissionChecker.hasPermission(ctx, "command", PermissionLevel.ALL))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(KissCommand::handleKiss)
                )
        );
    }

    private static int handleKiss(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        if (sourcePlayer == null) return 0;

        if (KissUtils.isSamePlayer(sourcePlayer, targetPlayer)) {
            sourcePlayer.sendSystemMessage(TextUtils.error(TextUtils.getTranslatableMessage("selfKissErrorMessage")));
            return 0;
        }

        Duration remainingCooldown = checkAndUpdateCooldown(sourcePlayer.getUUID());
        if (!remainingCooldown.isZero()) {
            sourcePlayer.sendSystemMessage(TextUtils.error(TextUtils.getTranslatableMessageWithNumber("cooldownErrorMessage", remainingCooldown.toSeconds())));
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

    private static void sendKiss(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) {
        Component messageToTarget = TextUtils.getTranslatableMessageWithPlayer("kiss", sourcePlayer);
        Component messageToSource = TextUtils.info(TextUtils.getTranslatableMessageWithPlayer("kissPromptMessage", targetPlayer));

        sourcePlayer.sendSystemMessage(messageToSource, false);
        targetPlayer.sendSystemMessage(messageToTarget, true);

        sourcePlayer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        targetPlayer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        if (KissUtils.isPlayerInvisible(targetPlayer)) return;

        KissUtils.spawnHeartParticlesForPlayer(targetPlayer.level(), targetPlayer, HEART_PARTICLE_COUNT, 0.3, 0.1);
    }
}
