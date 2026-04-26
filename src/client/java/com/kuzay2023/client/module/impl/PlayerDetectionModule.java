package com.kuzay2023.client.module.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

public class PlayerDetectionModule extends Module {
	private final NumberSetting range = addSetting(new NumberSetting("range", "Range", "General", 96.0, 16.0, 256.0, 1.0));
	private final BooleanSetting ignoreCreativeSpectator = addSetting(new BooleanSetting("ignore_creative_spectator", "Ignore Creative/Spectator", "General", true));
	private final Set<UUID> alertedPlayers = new HashSet<>();

	public PlayerDetectionModule() {
		super("player_detection", "Player Detection", "Warns you when players come within range.", "Other");
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			alertedPlayers.clear();
			return;
		}

		double maxDistance = range.getValue();
		Set<UUID> currentNearby = new HashSet<>();
		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == client.player) continue;
			if (ignoreCreativeSpectator.getValue() && (player.isCreative() || player.isSpectator())) continue;
			if (client.player.distanceTo(player) > maxDistance) continue;

			currentNearby.add(player.getUuid());
			if (alertedPlayers.add(player.getUuid())) {
				client.player.sendMessage(Text.literal("Player Detection: " + player.getName().getString() + " is nearby."), false);
			}
		}

		alertedPlayers.retainAll(currentNearby);
	}
}
