package com.kissmod.gametest;

import com.kissmod.config.KissConfigData;
import com.kissmod.config.KissConfigs;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder("kissmod")
public final class KissGameTest {
  @GameTest(template = "empty", timeoutTicks = 20)
  public static void kissCommandIsRegistered(GameTestHelper helper) {
    if (helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("kiss")
        == null) {
      helper.fail("The /kiss command was not registered");
      return;
    }

    helper.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 20)
  public static void configDefaultsArePublished(GameTestHelper helper) {
    var config = KissConfigs.server();

    if (!config.equals(KissConfigData.defaults())) {
      helper.fail("The default configuration snapshot was not published");
      return;
    }

    helper.succeed();
  }
}
