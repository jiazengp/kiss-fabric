// KissCommand.java
package com.kiss.command;

import com.kiss.config.KissConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KissCommand {
    private static final Map<UUID, Long> lastKissTimes = new HashMap<>();
    private static KissConfig config;

    public static void setConfig(KissConfig config) {
        KissCommand.config = config;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        if (!config.enableKissCommand) return;

        dispatcher.register(CommandManager.literal("kiss")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(KissCommand::executeKiss)
                )
        );
    }

    private static int executeKiss(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity sourcePlayer = source.getPlayer();
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");

        if (sourcePlayer == null) {
            return 0;
        }

        if (sourcePlayer.getUuid().equals(targetPlayer.getUuid())) {
            source.sendError(Text.literal("You can't kiss yourself!").formatted(Formatting.DARK_RED));
            return 0;
        }

        UUID sourceUUID = sourcePlayer.getUuid();
        long currentTime = System.currentTimeMillis();
        long lastKissTime = lastKissTimes.getOrDefault(sourceUUID, 0L);
        long cooldownMillis = config.commandCooldown * 1000L;

        if (currentTime - lastKissTime < cooldownMillis) {
            long remainingSeconds = (cooldownMillis - (currentTime - lastKissTime)) / 1000;
            source.sendError(Text.literal(String.format("You must wait %d seconds before kissing again!", remainingSeconds))
                    .formatted(Formatting.YELLOW));
            return 0;
        }

        // Send the kiss message
        Text kissMessage = buildMessage(config.kissMessage, sourcePlayer.getDisplayName(), Formatting.LIGHT_PURPLE);
        Text kissPromptMessage = buildMessage(config.kissPromptMessage, targetPlayer.getDisplayName(), Formatting.GRAY);
        sourcePlayer.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        targetPlayer.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        targetPlayer.sendMessage(kissMessage, false);
        sourcePlayer.sendMessage(kissPromptMessage, false);

        // Spawn heart particles
        ServerWorld world = sourcePlayer.getServerWorld();
        Vec3d pos = targetPlayer.getPos();
        world.spawnParticles(ParticleTypes.HEART,
                pos.x, pos.y + 1.6, pos.z,
                5, 0.3, 0.3, 0.3, 0.01);

        // Update cooldown
        lastKissTimes.put(sourceUUID, currentTime);

        return Command.SINGLE_SUCCESS;
    }

    private static MutableText buildMessage(String template, Text playerName, Formatting color) {
        String[] parts = template.split("%s", -1);
        MutableText result = Text.literal(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = result.append(playerName).append(Text.literal(parts[i]));
        }
        return result.formatted(color);
    }
}