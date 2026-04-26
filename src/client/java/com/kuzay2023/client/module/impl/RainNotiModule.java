package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class RainNotiModule extends Module {
	private final BooleanSetting notifyRainStart = addSetting(new BooleanSetting("notify_rain_start", "Notify Rain Start", "General", true));
	private final BooleanSetting notifyRainStop = addSetting(new BooleanSetting("notify_rain_stop", "Notify Rain Stop", "General", true));
	private Boolean lastRaining;

	public RainNotiModule() {
		super("rain_noti", "Rain Noti", "Shows a message when rain starts or stops.", "Other");
	}

	@Override
	protected void onDisable() {
		lastRaining = null;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			lastRaining = null;
			return;
		}

		boolean raining = client.world.isRaining();
		if (lastRaining == null) {
			lastRaining = raining;
			return;
		}

		if (raining != lastRaining) {
			if (raining && notifyRainStart.getValue()) {
				client.player.sendMessage(Text.literal("Rain Noti: It started raining."), false);
			} else if (!raining && notifyRainStop.getValue()) {
				client.player.sendMessage(Text.literal("Rain Noti: The rain stopped."), false);
			}
		}

		lastRaining = raining;
	}
}
