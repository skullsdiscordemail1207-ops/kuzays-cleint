package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

public class FullbrightModule extends Module {
	private final NumberSetting gammaLevel = addSetting(new NumberSetting("gamma_level", "Gamma Level", "General", 1.0, 0.0, 1.0, 0.01));
	private Double previousGamma;

	public FullbrightModule() {
		super("fullbright", "Fullbright", "Brightens dark areas by raising the Minecraft gamma option.", "Other");
	}

	@Override
	protected void onEnable() {
		applyGamma(MinecraftClient.getInstance());
	}

	@Override
	protected void onDisable() {
		restoreGamma(MinecraftClient.getInstance());
	}

	@Override
	public void tick(MinecraftClient client) {
		if (isEnabled()) applyGamma(client);
	}

	private void applyGamma(MinecraftClient client) {
		if (client == null || client.options == null) return;
		SimpleOption<Double> gamma = client.options.getGamma();
		if (previousGamma == null) previousGamma = gamma.getValue();
		double target = Math.max(0.0D, Math.min(1.0D, gammaLevel.getValue()));
		if (Double.compare(gamma.getValue(), target) != 0) gamma.setValue(target);
	}

	private void restoreGamma(MinecraftClient client) {
		if (client == null || client.options == null || previousGamma == null) return;
		client.options.getGamma().setValue(previousGamma);
		previousGamma = null;
	}
}
