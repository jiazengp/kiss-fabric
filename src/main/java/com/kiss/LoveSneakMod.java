package com.kiss;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoveSneakMod implements ModInitializer {

	private static final int TRIGGER_COUNT = 3;
	private static final int TIME_WINDOW_TICKS = 40;

	private static class SneakData {
		int count = 0;
		int lastTime = -TIME_WINDOW_TICKS * 2;
		boolean wasSneaking = false;
	}

	private final Map<UUID, SneakData> playerSneakData = new HashMap<>();

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int currentTick = server.getTicks();

			for (ServerWorld world : server.getWorlds()) {
				for (ServerPlayerEntity player : world.getPlayers()) {
					UUID uuid = player.getUuid();
					SneakData data = playerSneakData.computeIfAbsent(uuid, u -> new SneakData());

					boolean isSneaking = player.isSneaking();

					if (isSneaking && !data.wasSneaking) {
						int delta = currentTick - data.lastTime;

						if (delta <= TIME_WINDOW_TICKS) {
							data.count++;
						} else {
							data.count = 1;
						}

						data.lastTime = currentTick;

						if (data.count >= TRIGGER_COUNT) {
							Vec3d pos = player.getPos();
							Box box = new Box(pos.add(-5, -1, -5), pos.add(5, 2, 5));
							boolean foundNearby = false;

							for (ServerPlayerEntity other : world.getPlayers()) {
								if (!other.getUuid().equals(uuid) && box.contains(other.getPos())) {
									foundNearby = true;
									break;
								}
							}

							if (foundNearby) {
								world.spawnParticles(ParticleTypes.HEART, pos.x, pos.y + 2, pos.z, 5, 0.3, 0.3, 0.3, 0.01);
							}

							data.count = 0;
						}
					}

					data.wasSneaking = isSneaking;
				}
			}
		});
	}
}
