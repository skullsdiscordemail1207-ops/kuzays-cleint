package com.kuzay2023.client.module.impl.macro;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;

public class KeyboardMacroModule extends Module {
	private final StringSetting textSetting = addSetting(new StringSetting("macro_text", "Text", "General", "hello"));
	private final EnumSetting modeSetting = addSetting(new EnumSetting("macro_mode", "Mode", "General", "Chat", "Chat", "Command"));

	private boolean keyHeld;
	public KeyboardMacroModule(int index) {
		super("keyboard_macro_" + index, "Keyboard Macro " + index, "Press the keybind to send a saved chat message or command.", "Macros");
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

		boolean pressed = isBindPressed(client, boundKey);
		if (client.currentScreen != null && !isCtrlDown(client)) {
			pressed = false;
		}

		if (pressed && !keyHeld) {
			sendMacro(client.player);
		}

		keyHeld = pressed;
	}

	private void sendMacro(ClientPlayerEntity player) {
		if (player == null) {
			return;
		}

		String rawText = textSetting.getValue() == null ? "" : textSetting.getValue().trim();
		if (rawText.isEmpty()) {
			return;
		}

		if ("Command".equalsIgnoreCase(modeSetting.getValue())) {
			String command = rawText.startsWith("/") ? rawText.substring(1) : rawText;
			if (!command.isBlank()) {
				player.networkHandler.sendChatCommand(command);
			}
			return;
		}

		player.networkHandler.sendChatMessage(rawText);
	}

	private boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
}
