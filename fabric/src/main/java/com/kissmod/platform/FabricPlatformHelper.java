package com.kissmod.platform;

import com.kissmod.platform.services.IPlatformHelper;
import java.nio.file.Path;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlatformHelper implements IPlatformHelper {

  @Override
  public String getPlatformName() {
    return "Fabric";
  }

  @Override
  public boolean isModLoaded(String modId) {
    return FabricLoader.getInstance().isModLoaded(modId);
  }

  @Override
  public boolean isDevelopmentEnvironment() {
    return FabricLoader.getInstance().isDevelopmentEnvironment();
  }

  @Override
  public boolean isFakePlayer(ServerPlayer player) {
    return player instanceof FakePlayer;
  }

  @Override
  public boolean hasPermission(ServerPlayer player, String permissionPath) {
    return Permissions.check(player, "kissmod." + permissionPath, true);
  }

  @Override
  public Path getConfigDirectory() {
    return FabricLoader.getInstance().getConfigDir();
  }
}
