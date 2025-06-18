package com.kiss.command;

import com.kiss.config.KissConfig;
import com.kiss.utils.KissUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KissCommand {
    private static final Map<UUID, Long> lastKissTimes = new HashMap<>();
    private static KissConfig config;

    public static void setConfig(KissConfig config) {
        KissCommand.config = config;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        if (!config.enableKissCommand) return;

        dispatcher.register(CommandManager.literal("kiss")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(KissCommand::executeKiss)
                )
        );
    }

    private static int executeKiss(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity sourcePlayer = source.getPlayerOrThrow();
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");

        if (KissUtils.isSamePlayer(sourcePlayer, targetPlayer)) {
            KissUtils.sendError(source, config.selfKissErrorMessage, Formatting.DARK_RED);
            return 0;
        }

        if (!checkCooldown(sourcePlayer)) {
            long remaining = getRemainingCooldownSeconds(sourcePlayer);
            KissUtils.sendError(source, String.format(config.cooldownErrorMessage, remaining), Formatting.YELLOW);
            return 0;
        }

        sendKiss(sourcePlayer, targetPlayer);
        updateCooldown(sourcePlayer);

        return Command.SINGLE_SUCCESS;
    }

    private static void sendKiss(ServerPlayerEntity source, ServerPlayerEntity target) {
        Text msgToTarget = KissUtils.buildMessage(config.kissMessage, source.getDisplayName(), Formatting.LIGHT_PURPLE);
        Text msgToSource = KissUtils.buildMessage(config.kissPromptMessage, target.getDisplayName(), Formatting.GRAY);

        source.sendMessage(msgToSource, false);
        target.sendMessage(msgToTarget, false);

        source.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        target.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        KissUtils.spawnHeartParticles(source.getServerWorld(), target, 3, 0.3, 0.1);
    }

    private static boolean checkCooldown(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        long lastTime = lastKissTimes.getOrDefault(player.getUuid(), 0L);
        return now - lastTime >= config.commandCooldown * 1000L;
    }

    private static long getRemainingCooldownSeconds(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        long lastTime = lastKissTimes.getOrDefault(player.getUuid(), 0L);
        long remaining = config.commandCooldown * 1000L - (now - lastTime);
        return Math.max(0, remaining / 1000L);
    }

    private static void updateCooldown(ServerPlayerEntity player) {
        lastKissTimes.put(player.getUuid(), System.currentTimeMillis());
    }
}
