package com.kuzay2023.client.module.impl;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.module.Module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class KeyClearChatModule extends Module {
	private boolean keyHeld;

	public KeyClearChatModule() {
		super("key_clear_chat", "Key Clear Chat", "Clears your client chat when you press this module's keybind.", "Other");
		setEnabledSilently(true);
		setBoundKeySilently(GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void updateKeybind(MinecraftClient client) {
		int boundKey = getBoundKey();
		if (!isEnabled() || boundKey == GLFW.GLFW_KEY_UNKNOWN) {
			keyHeld = false;
			return;
		}

		boolean pressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey);
		if (client.currentScreen != null && !isCtrlDown(client)) {
			pressed = false;
		}

		if (pressed && !keyHeld && client.inGameHud != null) {
			client.inGameHud.getChatHud().clear(false);
		}

		keyHeld = pressed;
	}

	private boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
}
