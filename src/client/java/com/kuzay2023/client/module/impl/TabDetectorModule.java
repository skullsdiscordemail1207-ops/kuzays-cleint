package com.kuzay2023.client.module.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

public class TabDetectorModule extends Module {
	private final BooleanSetting notifyJoin = addSetting(new BooleanSetting("notify_join", "Notify Join", "General", true));
	private final BooleanSetting notifyLeave = addSetting(new BooleanSetting("notify_leave", "Notify Leave", "General", false));
	private final Set<UUID> seenPlayers = new HashSet<>();

	public TabDetectorModule() {
		super("tab_detector", "Tab Detector", "Tracks the tab list and tells you when new players show up there.", "Other");
	}

	@Override
	protected void onDisable() {
		seenPlayers.clear();
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null) {
			seenPlayers.clear();
			return;
		}

		Set<UUID> current = new HashSet<>();
		for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
			UUID uuid = entry.getProfile().getId();
			current.add(uuid);
			if (notifyJoin.getValue() && seenPlayers.add(uuid)) {
				client.player.sendMessage(Text.literal("Tab Detector: " + entry.getProfile().getName() + " appeared in tab."), false);
			}
		}

		if (notifyLeave.getValue()) {
			for (UUID uuid : new HashSet<>(seenPlayers)) {
				if (!current.contains(uuid)) {
					client.player.sendMessage(Text.literal("Tab Detector: a player left the tab list."), false);
				}
			}
		}

		seenPlayers.retainAll(current);
		seenPlayers.addAll(current);
	}
}
