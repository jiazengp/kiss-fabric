package com.kissmod.gametest;

import com.kissmod.config.KissConfigData;
import com.kissmod.config.KissConfigs;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class KissGameTest implements FabricGameTest {
  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 20)
  public void kissCommandIsRegistered(GameTestHelper helper) {
    if (helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("kiss")
        == null) {
      helper.fail("The /kiss command was not registered");
      return;
    }

    helper.succeed();
  }

  @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 20)
  public void configDefaultsArePublished(GameTestHelper helper) {
    var config = KissConfigs.server();

    if (!config.equals(KissConfigData.defaults())) {
      helper.fail("The default configuration snapshot was not published");
      return;
    }

    helper.succeed();
  }
}
