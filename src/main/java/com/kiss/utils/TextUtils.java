package com.kiss.utils;

import com.kiss.LoveSneakMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;

public class TextUtils {
    public static Text error(Text msg) {
        return Text.literal("").append(msg).formatted(Formatting.RED);
    }

    public static Text warning(Text msg) {
        return Text.literal("").append(msg).formatted(Formatting.YELLOW);
    }

    public static Text success(Text msg) {
        return Text.literal("").append(msg).formatted(Formatting.GREEN);
    }

    public static Text info(Text msg) {
        return Text.literal("").append(msg).formatted(Formatting.GRAY);
    }

    public static Text getTranslatableMessage(String key) {
        return Text.translatable(LoveSneakMod.MOD_ID + ".messages." + key);
    }

    public static Text getTranslatableMessageWithPlayer(String key, ServerPlayerEntity player) {
        return Text.translatable(LoveSneakMod.MOD_ID + ".messages." + key, getSafeDisplayName(player));
    }

    public static Text getTranslatableMessageWithNumber(String key, Long number) {
        return Text.translatable(LoveSneakMod.MOD_ID + ".messages." + key, number);
    }
    public static MutableText getSafeDisplayName(ServerPlayerEntity player) {
        if (player == null) return Text.translatable("argument.entity.notfound.player");
        Text displayName = Text.translatable("chat.type.text", player.getDisplayName(), "");
        return (MutableText) Objects.requireNonNullElseGet(displayName, player::getName);
    }
}
