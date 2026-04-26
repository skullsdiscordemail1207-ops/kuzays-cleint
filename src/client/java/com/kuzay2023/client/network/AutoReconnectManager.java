package com.kuzay2023.client.network;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.AutoReconnectModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class AutoReconnectManager {
	private static ServerInfo lastServerInfo;
	private static long reconnectAt;
	private static int reconnectAttempts;
	private static boolean disconnectScreenVisible;

	private AutoReconnectManager() {
	}

	public static void rememberLastServer(ServerInfo serverInfo) {
		if (serverInfo == null) {
			return;
		}
		lastServerInfo = serverInfo;
	}

	public static void onDisconnected() {
		disconnectScreenVisible = true;
		scheduleReconnect();
	}

	public static void scheduleReconnect() {
		AutoReconnectModule module = getModule();
		if (module == null || !module.isEnabled() || lastServerInfo == null) {
			reconnectAt = -1L;
			return;
		}
		if (reconnectAt > System.currentTimeMillis()) {
			return;
		}
		reconnectAttempts++;
		reconnectAt = System.currentTimeMillis() + (module.getDelaySeconds() * 1000L);
	}

	public static void cancelReconnect() {
		reconnectAt = -1L;
		reconnectAttempts = 0;
		disconnectScreenVisible = false;
	}

	public static void setDisconnectScreenVisible(boolean visible) {
		disconnectScreenVisible = visible;
		if (!visible && reconnectAt <= 0L) {
			reconnectAttempts = 0;
		}
	}

	public static boolean isAutoReconnectEnabled() {
		AutoReconnectModule module = getModule();
		return module != null && module.isEnabled();
	}

	public static void setAutoReconnectEnabled(boolean enabled) {
		AutoReconnectModule module = getModule();
		if (module == null) {
			return;
		}
		module.setEnabled(enabled);
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context != null) {
			context.configManager().save(context);
		}
	}

	public static int getSecondsRemaining() {
		if (reconnectAt <= 0L) {
			return -1;
		}
		return Math.max(0, (int) Math.ceil((reconnectAt - System.currentTimeMillis()) / 1000.0));
	}

	public static void tick(MinecraftClient client) {
		if (client == null) {
			return;
		}

		if (!isAutoReconnectEnabled()) {
			cancelReconnect();
			return;
		}

		if (client.currentScreen instanceof DisconnectedScreen) {
			disconnectScreenVisible = true;
			if (reconnectAt <= 0L && lastServerInfo != null) {
				scheduleReconnect();
			}
		} else if (client.world != null || client.getNetworkHandler() != null) {
			reconnectAt = -1L;
			reconnectAttempts = 0;
			disconnectScreenVisible = false;
			return;
		}

		if (!disconnectScreenVisible || reconnectAt <= 0L || System.currentTimeMillis() < reconnectAt || lastServerInfo == null) {
			return;
		}
		if (!(client.currentScreen instanceof DisconnectedScreen)) {
			return;
		}

		reconnectAt = -1L;
		Screen parent = client.currentScreen;
		if (parent == null) {
			parent = new MultiplayerScreen(null);
		}
		ConnectScreen.connect(parent, client, ServerAddress.parse(lastServerInfo.address), lastServerInfo, false, null);
	}

	private static AutoReconnectModule getModule() {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return null;
		}
		Module module = context.moduleManager().getModule("auto_reconnect");
		return module instanceof AutoReconnectModule autoReconnectModule ? autoReconnectModule : null;
	}
}
