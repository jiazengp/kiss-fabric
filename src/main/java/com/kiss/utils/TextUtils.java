package com.kiss.utils;

import com.kiss.LoveSneakMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.Objects;

public class TextUtils {
    public static Component error(Component msg) {
        return Component.literal("").append(msg).withStyle(ChatFormatting.RED);
    }

    public static Component warning(Component msg) {
        return Component.literal("").append(msg).withStyle(ChatFormatting.YELLOW);
    }

    public static Component success(Component msg) {
        return Component.literal("").append(msg).withStyle(ChatFormatting.GREEN);
    }

    public static Component info(Component msg) {
        return Component.literal("").append(msg).withStyle(ChatFormatting.GRAY);
    }

    public static Component getTranslatableMessage(String key) {
        return Component.translatable(LoveSneakMod.MOD_ID + ".messages." + key);
    }

    public static Component getTranslatableMessageWithPlayer(String key, ServerPlayer player) {
        return Component.translatable(LoveSneakMod.MOD_ID + ".messages." + key, getSafeDisplayName(player));
    }

    public static Component getTranslatableMessageWithNumber(String key, Long number) {
        return Component.translatable(LoveSneakMod.MOD_ID + ".messages." + key, number);
    }
    public static MutableComponent getSafeDisplayName(ServerPlayer player) {
        if (player == null) return Component.translatable("argument.entity.notfound.player");
        Component displayName = Component.translatable("chat.type.text", player.getDisplayName(), "");
        return (MutableComponent) Objects.requireNonNullElseGet(displayName, player::getName);
    }
}
