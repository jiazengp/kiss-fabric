package com.kiss.utils;


import com.kiss.LoveSneakMod;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

public class PermissionChecker {
    public static boolean hasPermission(SharedSuggestionProvider ctx, String permission, PermissionLevel fallbackLevel) {
        return Permissions.check(ctx,LoveSneakMod.MOD_ID.toLowerCase() + "." + permission, fallbackLevel);
    }

    public static boolean hasPermission(ServerPlayer player, String permission, PermissionLevel fallbackLevel) {
        return Permissions.check(player, LoveSneakMod.MOD_ID.toLowerCase() + "." + permission, fallbackLevel);
    }
}
