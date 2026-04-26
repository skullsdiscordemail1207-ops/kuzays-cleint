package com.kuzay2023.client.module.impl.macro;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;

public class KeyboardMacrosModule extends Module {
	private static final int MAX_MACRO_COUNT = 12;
	private final NumberSetting activeMacrosSetting = addSetting(new NumberSetting("macro_slots", "Macro Slots", "General", 4.0, 1.0, MAX_MACRO_COUNT, 1.0));

	private final StringSetting[] textSettings = new StringSetting[MAX_MACRO_COUNT];
	private final EnumSetting[] modeSettings = new EnumSetting[MAX_MACRO_COUNT];
	private final StringSetting[] bindSettings = new StringSetting[MAX_MACRO_COUNT];
	private final boolean[] keyHeld = new boolean[MAX_MACRO_COUNT];

	public KeyboardMacrosModule() {
		super("keyboard_macros", "Keybaord Macros", "Stores four keyboard macros under one module.", "Macros");
		setEnabledSilently(true);
		setBoundKeySilently(GLFW.GLFW_KEY_UNKNOWN);

		for (int i = 0; i < MAX_MACRO_COUNT; i++) {
			int slot = i + 1;
			String section = "Macro " + slot;
			textSettings[i] = addSetting(new StringSetting("macro_" + slot + "_text", "Text", section, ""));
			modeSettings[i] = addSetting(new EnumSetting("macro_" + slot + "_mode", "Mode", section, "Chat", "Chat", "Command"));
			bindSettings[i] = addSetting(new StringSetting("macro_" + slot + "_key", "Keybind", section, Integer.toString(GLFW.GLFW_KEY_UNKNOWN)));
		}
	}

	@Override
	public void updateKeybind(MinecraftClient client) {
		// This module manages its own four binds, so the top-level module bind stays unused.
	}

	@Override
	public void tick(MinecraftClient client) {
		if (!isEnabled()) {
			for (int i = 0; i < MAX_MACRO_COUNT; i++) keyHeld[i] = false;
			return;
		}

		for (int i = 0; i < getActiveMacroCount(); i++) {
			int boundKey = getMacroKey(i);
			if (boundKey == GLFW.GLFW_KEY_UNKNOWN) {
				keyHeld[i] = false;
				continue;
			}

			boolean pressed = isBindPressed(client, boundKey);
			if (client.currentScreen != null && !isCtrlDown(client)) {
				pressed = false;
			}

			if (pressed && !keyHeld[i]) {
				sendMacro(client.player, i);
			}

			keyHeld[i] = pressed;
		}
	}

	public int getActiveMacroCount() {
		return Math.max(1, Math.min(MAX_MACRO_COUNT, (int) Math.round(activeMacrosSetting.getValue())));
	}

	public boolean isMacroActive(int index) {
		return index >= 0 && index < getActiveMacroCount();
	}

	public boolean canAddMacro() {
		return getActiveMacroCount() < MAX_MACRO_COUNT;
	}

	public void addMacro() {
		if (canAddMacro()) {
			activeMacrosSetting.setValue((double) (getActiveMacroCount() + 1));
		}
	}

	public int getMacroKey(int index) {
		if (index < 0 || index >= MAX_MACRO_COUNT) return GLFW.GLFW_KEY_UNKNOWN;
		try {
			return Integer.parseInt(bindSettings[index].getValue());
		} catch (NumberFormatException ignored) {
			return GLFW.GLFW_KEY_UNKNOWN;
		}
	}

	public void setMacroKey(int index, int keyCode, int scanCode) {
		if (index < 0 || index >= MAX_MACRO_COUNT) return;
		int boundKey = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
		bindSettings[index].setValue(Integer.toString(boundKey));
	}

	public void setMacroMouseButton(int index, int mouseButton) {
		if (index < 0 || index >= MAX_MACRO_COUNT) return;
		bindSettings[index].setValue(Integer.toString(Module.encodeMouseButton(mouseButton)));
	}

	public String getMacroKeyName(int index) {
		return Module.describeBoundKey(getMacroKey(index));
	}

	private void sendMacro(ClientPlayerEntity player, int index) {
		if (player == null || index < 0 || index >= MAX_MACRO_COUNT) {
			return;
		}

		String rawText = textSettings[index].getValue() == null ? "" : textSettings[index].getValue().trim();
		if (rawText.isEmpty()) {
			return;
		}

		if ("Command".equalsIgnoreCase(modeSettings[index].getValue())) {
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
