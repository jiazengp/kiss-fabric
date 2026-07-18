package com.kissmod.gametest;

import com.kissmod.config.KissConfigData;
import com.kissmod.config.KissConfigs;
import java.util.Objects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

public final class KissGameTest {
  @GameTest(maxTicks = 20)
  public void kissCommandIsRegistered(GameTestHelper helper) {
    var level = Objects.requireNonNull(helper.getLevel(), "The GameTest level was not available");
    var server = Objects.requireNonNull(level.getServer(), "The GameTest server was not available");

    if (server.getCommands().getDispatcher().getRoot().getChild("kiss") == null) {
      helper.fail(Component.literal("The /kiss command was not registered"));
      return;
    }

    helper.succeed();
  }

  @GameTest(maxTicks = 20)
  public void configDefaultsArePublished(GameTestHelper helper) {
    var config = KissConfigs.server();

    if (!config.equals(KissConfigData.defaults())) {
      helper.fail(Component.literal("The default configuration snapshot was not published"));
      return;
    }

    helper.succeed();
  }
}
