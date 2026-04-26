package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.balance.BalanceHooks;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;

import net.minecraft.client.MinecraftClient;

public class FakeBalanceModule extends Module {
	private final BooleanSetting refreshOnEnable = addSetting(new BooleanSetting("refresh_on_enable", "Refresh Real Balance", "General", true));

	public FakeBalanceModule() {
		super("fake_balance", "Fake Balance", "Replaces /bal output with the fake scoreboard balance while keeping track of your real one.", "Other");
		setEnabledSilently(true);
	}

	@Override
	protected void onEnable() {
		if (refreshOnEnable.getValue()) {
			BalanceHooks.requestSilentRealBalance(MinecraftClient.getInstance());
		}
	}
}
