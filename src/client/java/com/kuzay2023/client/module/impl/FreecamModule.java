package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.freecam.FreecamController;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;

public class FreecamModule extends Module {
	private final NumberSetting movementSpeed = addSetting(new NumberSetting("movement_speed", "Movement Speed", "General", 1.8, 0.5, 8.0, 0.1));
	private final NumberSetting verticalSpeed = addSetting(new NumberSetting("vertical_speed", "Vertical Speed", "Advanced", 1.2, 0.2, 6.0, 0.1));
	private final BooleanSetting collision = addSetting(new BooleanSetting("collision", "Collision", "Advanced", false));
	private final BooleanSetting smoothing = addSetting(new BooleanSetting("camera_smoothing", "Camera Smoothing", "Advanced", true));

	public FreecamModule() {
		super("freecam", "Freecam", "Moves your camera independently from your player with smooth six-direction movement.", "Other");
	}

	@Override
	protected void onEnable() {
		FreecamController.activate(MinecraftClient.getInstance(), movementSpeed.getValue(), verticalSpeed.getValue(), collision.getValue(), smoothing.getValue());
	}

	@Override
	protected void onDisable() {
		FreecamController.deactivate(MinecraftClient.getInstance());
	}

	@Override
	public void tick(MinecraftClient client) {
		FreecamController.updateSettings(movementSpeed.getValue(), verticalSpeed.getValue(), collision.getValue(), smoothing.getValue());
		FreecamController.tick(client);
	}
}
