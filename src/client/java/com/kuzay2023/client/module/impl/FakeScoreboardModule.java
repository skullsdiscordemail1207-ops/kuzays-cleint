package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.fakescoreboard.FakeScoreboardClientHooks;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardConfig;
import com.kuzay2023.client.module.Module;

public class FakeScoreboardModule extends Module {
	public FakeScoreboardModule() {
		super("fake_scoreboard", "Fake Scoreboard", "Uses your original fake scoreboard override and config screen.", "Other");
		setEnabledSilently(true);
	}

	@Override
	protected void onEnable() {
		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		if (config != null) {
			config.setEnabled(true);
			config.save();
		}
	}

	@Override
	protected void onDisable() {
		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		if (config != null) {
			config.setEnabled(false);
			config.save();
		}
	}
}
