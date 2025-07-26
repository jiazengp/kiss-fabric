package com.kiss.utils;


import com.kiss.LoveSneakMod;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionChecker {
    public static boolean hasPermission(CommandSource ctx, String permission, int fallbackLevel) {
        return Permissions.check(ctx,LoveSneakMod.MOD_ID.toLowerCase() + "." + permission, fallbackLevel);
    }

    public static boolean hasPermission(ServerPlayerEntity player, String permission, int fallbackLevel) {
        return Permissions.check(player, LoveSneakMod.MOD_ID.toLowerCase() + "." + permission, fallbackLevel);
    }
}
