package com.kuzay2023.client.module.impl.gamble;

import com.kuzay2023.client.gamble.GambleListerService;
import com.kuzay2023.client.module.Module;

public class GamblesListerModule extends Module {
	public GamblesListerModule() {
		super("gambles_lister", "Payment Lister", "Tracks recent payments and shows them in a HUD list.", "Gambles");
	}

	@Override
	protected void onEnable() {
		GambleListerService.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		GambleListerService.setEnabled(false);
	}
}
