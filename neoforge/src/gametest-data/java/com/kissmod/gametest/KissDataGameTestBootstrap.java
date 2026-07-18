package com.kissmod.gametest;

import com.kissmod.Constants;
import com.kissmod.config.KissConfigData;
import com.kissmod.config.KissConfigs;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Entry point reserved for registry-based NeoForge GameTests. */
public final class KissDataGameTestBootstrap {
  private static final ResourceLocation TEST_ID =
      ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "kiss_command_registered");
  private static final ResourceLocation CONFIG_TEST_ID =
      ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "config_defaults_published");

  public static void init() {}

  private KissDataGameTestBootstrap() {}

  public static void register(RegisterGameTestsEvent event) {
    Holder<TestEnvironmentDefinition> environment =
        event.registerEnvironment(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "empty"),
            new TestEnvironmentDefinition.AllOf());
    TestData<Holder<TestEnvironmentDefinition>> data =
        new TestData<>(
            environment,
            ResourceLocation.withDefaultNamespace("empty"),
            20,
            0,
            true,
            Rotation.NONE);
    event.registerTest(TEST_ID, new KissCommandTestInstance(data));
    event.registerTest(CONFIG_TEST_ID, new KissConfigTestInstance(data));
  }

  private static void checkCommand(GameTestHelper helper) {
    if (helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("kiss")
        == null) {
      helper.fail(Component.literal("The /kiss command was not registered"));
      return;
    }
    helper.succeed();
  }

  private static final class KissCommandTestInstance extends GameTestInstance {
    private KissCommandTestInstance(TestData<Holder<TestEnvironmentDefinition>> data) {
      super(data);
    }

    @Override
    public void run(GameTestHelper helper) {
      checkCommand(helper);
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends GameTestInstance> codec() {
      return com.mojang.serialization.MapCodec.unit(this);
    }

    @Override
    protected MutableComponent typeDescription() {
      return Component.empty();
    }
  }

  private static void checkConfigDefaults(GameTestHelper helper) {
    var config = KissConfigs.server();

    if (!config.equals(KissConfigData.defaults())) {
      helper.fail(Component.literal("The default configuration snapshot was not published"));
      return;
    }
    helper.succeed();
  }

  private static final class KissConfigTestInstance extends GameTestInstance {
    private KissConfigTestInstance(TestData<Holder<TestEnvironmentDefinition>> data) {
      super(data);
    }

    @Override
    public void run(GameTestHelper helper) {
      checkConfigDefaults(helper);
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends GameTestInstance> codec() {
      return com.mojang.serialization.MapCodec.unit(this);
    }

    @Override
    protected MutableComponent typeDescription() {
      return Component.empty();
    }
  }
}
