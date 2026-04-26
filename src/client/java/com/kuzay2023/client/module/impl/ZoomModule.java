package com.kuzay2023.client.module.impl;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class ZoomModule extends Module {
	private static final String HOLD_MODE = "Hold";
	private static final String TOGGLE_MODE = "Toggle";

	private final EnumSetting bindMode = addSetting(new EnumSetting("bind_mode", "Bind Mode", "General", HOLD_MODE, HOLD_MODE, TOGGLE_MODE));
	private final NumberSetting zoomFov = addSetting(new NumberSetting("zoom_fov", "Zoom Fov", "General", 18.0, 0.5, 90.0, 0.5));
	private final NumberSetting scrollStep = addSetting(new NumberSetting("scroll_step", "Scroll Step", "General", 1.0, 0.25, 8.0, 0.25));

	private boolean bindHeld;
	private Boolean previousSmoothCamera;
	private double activeZoomFov;

	public ZoomModule() {
		super("zoom", "Zoom", "Zooms your camera in without needing any outside client.", "Other");
	}

	@Override
	protected void onEnable() {
		bindHeld = false;
		activeZoomFov = zoomFov.clamp(zoomFov.getValue());
		applyZoom(MinecraftClient.getInstance());
	}

	@Override
	protected void onDisable() {
		bindHeld = false;
		restoreFov(MinecraftClient.getInstance());
	}

	@Override
	public void updateKeybind(MinecraftClient client) {
		int boundKey = getBoundKey();
		if (boundKey == GLFW.GLFW_KEY_UNKNOWN) {
			bindHeld = false;
			if (isHoldMode() && isEnabled()) {
				setEnabled(false);
			} else {
				restoreFov(client);
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
		if (!isEnabled()) {
			restoreFov(client);
		}
	}

	@Override
	public void tick(MinecraftClient client) {
		if (isEnabled()) {
			applyZoom(client);
		} else {
			restoreFov(client);
		}
	}

	private void applyZoom(MinecraftClient client) {
		if (client == null || client.options == null) {
			return;
		}
		if (activeZoomFov <= 0.0) {
			activeZoomFov = zoomFov.clamp(zoomFov.getValue());
		}
		if (previousSmoothCamera == null) {
			previousSmoothCamera = client.options.smoothCameraEnabled;
		}
		client.options.smoothCameraEnabled = true;
	}

	private void restoreFov(MinecraftClient client) {
		if (client == null || client.options == null) {
			return;
		}
		if (previousSmoothCamera != null) {
			client.options.smoothCameraEnabled = previousSmoothCamera;
		}
		previousSmoothCamera = null;
	}

	private boolean isHoldMode() {
		return HOLD_MODE.equals(bindMode.getValue());
	}

	public boolean handleScroll(double verticalAmount) {
		if (!isEnabled() || verticalAmount == 0.0) {
			return false;
		}

		double nextValue = activeZoomFov - (Math.signum(verticalAmount) * scrollStep.getValue());
		activeZoomFov = zoomFov.clamp(nextValue);
		zoomFov.setValue(activeZoomFov);
		applyZoom(MinecraftClient.getInstance());
		return true;
	}

	public double getActiveZoomFov() {
		if (activeZoomFov <= 0.0) {
			activeZoomFov = zoomFov.clamp(zoomFov.getValue());
		}
		return activeZoomFov;
	}

	private boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
}
