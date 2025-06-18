package com.kiss;

import com.kiss.command.KissCommand;
import com.kiss.config.KissConfig;
import com.kiss.utils.KissUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoveSneakMod implements ModInitializer {
	private static final int TRIGGER_COUNT = 3;
	private static final int TIME_WINDOW_TICKS = 40;
	public static final String MOD_ID = "kissmod";
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

	public KissConfig config;

	private static class SneakData {
		int count = 0;
		int lastTime = -TIME_WINDOW_TICKS * 2;
		int particleLevel = 1;
		boolean wasSneaking = false;
	}

	private final Map<UUID, SneakData> playerSneakData = new HashMap<>();

	@Override
	public void onInitialize() {
		config = KissConfig.load();
		config.save();
		KissCommand.setConfig(config);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int currentTick = server.getTicks();
			for (ServerWorld world : server.getWorlds()) {
				for (ServerPlayerEntity player : world.getPlayers()) {
					handleSneak(player, world, currentTick);
				}
			}
		});

		CommandRegistrationCallback.EVENT.register(KissCommand::register);
	}

	private void handleSneak(ServerPlayerEntity player, ServerWorld world, int currentTick) {
		UUID uuid = player.getUuid();
		SneakData data = playerSneakData.computeIfAbsent(uuid, u -> new SneakData());
		boolean isSneaking = player.isSneaking();

		if (isSneaking && !data.wasSneaking) {
			int delta = currentTick - data.lastTime;
			data.count = (delta <= TIME_WINDOW_TICKS) ? data.count + 1 : 1;
			data.lastTime = currentTick;

			if (data.count >= TRIGGER_COUNT) {
				if (KissUtils.hasVisibleNearbyPlayers(player, world, config.sneakTriggerRadius, config.maxViewAngleDegree)) {
					data.particleLevel = Math.min(config.maxSneakParticles, data.particleLevel + 1);
					KissUtils.spawnHeartParticles(world, player, data.particleLevel + 1, 0.3, 0.1);
				}
				data.count = 0;
			} else if (KissUtils.hasVisibleNearbyPlayers(player, world, 6, config.maxViewAngleDegree)) {
				KissUtils.spawnHeartParticles(world, player, 1, 0.1, 0.05);
			}
		}

		if (!isSneaking && data.wasSneaking) {
			data.particleLevel = Math.max(1, data.particleLevel - 2);
		}

		data.wasSneaking = isSneaking;
	}
}
