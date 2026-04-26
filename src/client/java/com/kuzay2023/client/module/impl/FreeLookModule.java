package com.kuzay2023.client.module.impl;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.freelook.FreeLookController;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.EnumSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class FreeLookModule extends Module {
	private static final String HOLD_MODE = "Hold";
	private static final String TOGGLE_MODE = "Toggle";

	private final EnumSetting bindMode = addSetting(new EnumSetting("bind_mode", "Bind Mode", "General", HOLD_MODE, HOLD_MODE, TOGGLE_MODE));

	private boolean bindHeld;

	public FreeLookModule() {
		super("free_look", "Free Look", "Moves the camera without turning your player. Use the module keybind and pick Hold or Toggle in the options.", "Other");
	}

	@Override
	protected void onEnable() {
		bindHeld = false;
		FreeLookController.activate(MinecraftClient.getInstance());
	}

	@Override
	protected void onDisable() {
		bindHeld = false;
		FreeLookController.deactivate(MinecraftClient.getInstance());
	}

	@Override
	public void updateKeybind(MinecraftClient client) {
		int boundKey = getBoundKey();
		if (boundKey == GLFW.GLFW_KEY_UNKNOWN) {
			bindHeld = false;
			if (isHoldMode() && isEnabled()) {
				setEnabled(false);
			}
			return;
		}

		boolean pressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey);
		if (client.currentScreen != null && !isCtrlDown(client)) {
			pressed = false;
		}

		if (isHoldMode()) {
			if (pressed != isEnabled()) {
				setEnabled(pressed);
			}
		} else if (pressed && !bindHeld) {
			toggle();
		}

		bindHeld = pressed;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (!isEnabled()) {
			FreeLookController.deactivate(client);
			return;
		}

		if (client.player == null || client.world == null) {
			FreeLookController.deactivate(client);
			return;
		}

		if (!FreeLookController.isActive()) {
			FreeLookController.activate(client);
		}
		FreeLookController.updateTargetedPlayer(client);
	}

	private boolean isHoldMode() {
		return HOLD_MODE.equals(bindMode.getValue());
	}

	private boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
}
