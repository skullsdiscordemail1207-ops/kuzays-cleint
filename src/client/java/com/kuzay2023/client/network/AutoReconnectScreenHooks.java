package com.kuzay2023.client.network;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class AutoReconnectScreenHooks {
	private AutoReconnectScreenHooks() {
	}

	public static void initialize() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof DisconnectedScreen)) {
				return;
			}

			AutoReconnectManager.setDisconnectScreenVisible(true);
			if (AutoReconnectManager.isAutoReconnectEnabled()) {
				AutoReconnectManager.scheduleReconnect();
			}
			ButtonWidget button = ButtonWidget.builder(Text.literal(buttonLabel()), widget -> {
				AutoReconnectManager.setAutoReconnectEnabled(!AutoReconnectManager.isAutoReconnectEnabled());
				if (AutoReconnectManager.isAutoReconnectEnabled()) {
					AutoReconnectManager.scheduleReconnect();
				} else {
					AutoReconnectManager.cancelReconnect();
				}
				widget.setMessage(Text.literal(buttonLabel()));
			}).dimensions(screen.width / 2 - 100, Math.max(8, screen.height - 52), 200, 20).build();

			Screens.getButtons(screen).add(button);
			ScreenEvents.afterTick(screen).register(current -> button.setMessage(Text.literal(buttonLabel())));
			ScreenEvents.remove(screen).register(AutoReconnectScreenHooks::onScreenRemoved);
		});
	}

	private static void onScreenRemoved(Screen screen) {
		AutoReconnectManager.setDisconnectScreenVisible(false);
	}

	private static String buttonLabel() {
		if (!AutoReconnectManager.isAutoReconnectEnabled()) {
			return "Auto Reconnect: OFF";
		}
		int seconds = AutoReconnectManager.getSecondsRemaining();
		return seconds >= 0 ? "Auto Reconnect: ON (" + seconds + "s)" : "Auto Reconnect: ON";
	}
}
