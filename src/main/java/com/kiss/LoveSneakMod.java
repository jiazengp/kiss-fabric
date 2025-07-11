package com.kiss;

import com.kiss.command.KissCommand;
import com.kiss.config.KissConfig;
import com.kiss.manager.SneakManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoveSneakMod implements ModInitializer {
	public static final String MOD_ID = "kissmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SneakManager sneakManager;

	@Override
	public void onInitialize() {
        KissConfig config = KissConfig.load();
		config.save();

		sneakManager = new SneakManager(config);
		KissCommand.setConfig(config);

		ServerTickEvents.END_SERVER_TICK.register(server ->
				sneakManager.onEndServerTick(server, server.getTicks())
		);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				sneakManager.onPlayerLeave(handler.getPlayer().getUuid())
		);

		CommandRegistrationCallback.EVENT.register(KissCommand::register);

		LOGGER.info("[{}] Initialized", MOD_ID);
	}
}
