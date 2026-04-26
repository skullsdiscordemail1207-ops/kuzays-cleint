package com.kuzay2023.client.module.impl;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.money.MoneyModuleUtil;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class SetHomeHiddenModule extends Module {
	private static final long FEEDBACK_SUPPRESSION_WINDOW_MS = 2_000L;

	private static long suppressFeedbackUntil;
	private static boolean feedbackSuppressionEnabled;

	private final StringSetting setHomeKey = addSetting(new StringSetting("sethome_key", "Set Home Key", "General", "key.keyboard.i"));
	private boolean setHomeKeyHeld;

	public SetHomeHiddenModule() {
		super("sethome_hidden", "Set Home Hidden", "Runs /sethome on a hotkey and hides the feedback message.", "Other");
		setEnabledSilently(true);
	}

	@Override
	protected void onEnable() {
		feedbackSuppressionEnabled = true;
	}

	@Override
	protected void onDisable() {
		feedbackSuppressionEnabled = false;
		setHomeKeyHeld = false;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null || client.currentScreen != null) {
			setHomeKeyHeld = false;
			return;
		}

		boolean pressed = isSetHomeKeyPressed(client);
		if (pressed && !setHomeKeyHeld) {
			suppressFeedbackUntil = System.currentTimeMillis() + FEEDBACK_SUPPRESSION_WINDOW_MS;
			MoneyModuleUtil.sendCommand(client, "sethome");
		}
		setHomeKeyHeld = pressed;
	}

	public String getSetHomeKeyName() {
		InputUtil.Key key = InputUtil.fromTranslationKey(setHomeKey.getValue());
		return key == InputUtil.UNKNOWN_KEY ? Module.UNBOUND_BIND_TEXT : key.getLocalizedText().getString();
	}

	public void setSetHomeKey(int keyCode, int scanCode) {
		setHomeKey.setValue(keyCode == GLFW.GLFW_KEY_ESCAPE ? "key.keyboard.unknown" : InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey());
		setHomeKeyHeld = false;
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().configManager().save(KuzayClientModClient.getContext());
		}
	}

	public static boolean shouldSuppressMessage(String rawMessage) {
		if (!feedbackSuppressionEnabled || rawMessage == null || System.currentTimeMillis() > suppressFeedbackUntil) {
			return false;
		}

		String normalized = rawMessage.trim().toLowerCase(Locale.ROOT).replaceAll("[!.]+$", "");
		return normalized.equals("home set") || normalized.equals("set home");
	}

	private boolean isSetHomeKeyPressed(MinecraftClient client) {
		InputUtil.Key key = InputUtil.fromTranslationKey(setHomeKey.getValue());
		return key != InputUtil.UNKNOWN_KEY && InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
	}
}
