package com.kissmod.platform;

import com.kissmod.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public class NeoForgePlatformHelper implements IPlatformHelper {
  private static final PermissionNode<Boolean> COMMAND_PERMISSION = permissionNode("command");
  private static final PermissionNode<Boolean> TRIGGER_PERMISSION = permissionNode("trigger");

  public NeoForgePlatformHelper() {
    NeoForge.EVENT_BUS.addListener(this::registerPermissionNodes);
  }

  @Override
  public String getPlatformName() {
    return "NeoForge";
  }

  @Override
  public boolean isModLoaded(String modId) {
    return ModList.get().isLoaded(modId);
  }

  @Override
  public boolean isDevelopmentEnvironment() {
    return NeoForgeLoaderCompat.isDevelopmentEnvironment();
  }

  @Override
  public boolean isFakePlayer(ServerPlayer player) {
    return player instanceof FakePlayer;
  }

  @Override
  public boolean hasPermission(ServerPlayer player, String permissionPath) {
    PermissionNode<Boolean> permission =
        switch (permissionPath) {
          case "command" -> COMMAND_PERMISSION;
          case "trigger" -> TRIGGER_PERMISSION;
          default -> null;
        };
    return permission != null && PermissionAPI.getPermission(player, permission);
  }

  private static PermissionNode<Boolean> permissionNode(String path) {
    return new PermissionNode<>(
        "kissmod", path, PermissionTypes.BOOLEAN, (player, uuid, contexts) -> true);
  }

  private void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
    event.addNodes(COMMAND_PERMISSION, TRIGGER_PERMISSION);
  }

  @Override
  public Path getConfigDirectory() {
    return FMLPaths.CONFIGDIR.get();
  }
}
