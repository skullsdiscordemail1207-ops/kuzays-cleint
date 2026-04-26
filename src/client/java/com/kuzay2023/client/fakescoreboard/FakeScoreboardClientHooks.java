package com.kuzay2023.client.fakescoreboard;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public final class FakeScoreboardClientHooks {
	private static FakeScoreboardConfig config;
	private static KeyBinding openScreenKey;
	private static boolean initialized;
	private static boolean openScreenKeyHeld;

	private FakeScoreboardClientHooks() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		config = FakeScoreboardConfig.load();
		openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.kuzay2023sclient.fake_scoreboard_config",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_BRACKET,
			"category.kuzay2023sclient.client"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (consumeOpenScreenKey(client)) {
				client.setScreen(new FakeScoreboardScreen(client.currentScreen));
			}
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleMessage(message));
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleMessage(message));
	}

	private static void handleMessage(Text message) {
		MoneyParser.parseDelta(message.getString()).ifPresent(delta -> {
			config.changeMoney(delta);
			config.save();
		});
	}

	public static FakeScoreboardConfig getConfig() {
		return config;
	}

	private static boolean consumeOpenScreenKey(MinecraftClient client) {
		InputUtil.Key boundKey = InputUtil.fromTranslationKey(openScreenKey.getBoundKeyTranslationKey());
		if (boundKey == InputUtil.UNKNOWN_KEY) {
			openScreenKeyHeld = false;
			return false;
		}

		boolean pressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey.getCode());
		if (client.currentScreen != null && !isCtrlDown(client)) {
			pressed = false;
		}

		boolean triggered = pressed && !openScreenKeyHeld;
		openScreenKeyHeld = pressed;
		return triggered;
	}

	private static boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
}
