package com.kuzay2023.client.module.impl.gamble;

import com.kuzay2023.client.gamble.HighestGamblersService;
import com.kuzay2023.client.module.Module;

public class HighestGamblersModule extends Module {
	public HighestGamblersModule() {
		super("highest_gamblers", "Highest Payments", "Tracks the top payments you have received and renders a leaderboard HUD.", "Gambles");
	}

	@Override
	protected void onEnable() {
		HighestGamblersService.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		HighestGamblersService.setEnabled(false);
	}
}
