package com.kuzay2023.client.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.kuzay2023.client.KuzayClientContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class ServerTabManager {
	public static final String ALL_TAB_ID = "all";
	private static final String GLOBAL_PREFIX = "global|";
	private static final String SERVER_PREFIX = "server|";

	private ServerTabManager() {
	}

	public static List<ClientTab> visibleTabs(KuzayClientContext context, MinecraftClient client) {
		List<ClientTab> tabs = new ArrayList<>();
		tabs.add(new ClientTab(ALL_TAB_ID, "All", "All", "", false, true));

		String serverKey = currentServerKey(client);
		String serverLabel = currentServerLabel(client);
		for (String category : context.moduleManager().getCategories()) {
			tabs.add(globalTab(category));
			if (!serverKey.isBlank() && !"VIP".equals(category)) {
				tabs.add(serverTab(category, serverKey, serverLabel));
			}
		}

		return tabs;
	}

	public static List<ClientTab> globalTabs(KuzayClientContext context) {
		List<ClientTab> tabs = new ArrayList<>();
		tabs.add(new ClientTab(ALL_TAB_ID, "All", "All", "", false, true));
		for (String category : context.moduleManager().getCategories()) {
			tabs.add(globalTab(category));
		}
		return tabs;
	}

	public static ClientTab globalTab(String category) {
		return new ClientTab(globalTabId(category), category, category, "", "VIP".equals(category), false);
	}

	public static ClientTab serverTab(String category, String serverKey, String serverLabel) {
		String shortLabel = serverLabel == null || serverLabel.isBlank() ? serverKey : serverLabel;
		return new ClientTab(serverTabId(category, serverKey), category + " [" + shortLabel + "]", category, serverKey, false, false);
	}

	public static ClientTab findTab(KuzayClientContext context, MinecraftClient client, String tabId) {
		for (ClientTab tab : visibleTabs(context, client)) {
			if (tab.id().equals(tabId)) {
				return tab;
			}
		}
		return new ClientTab(ALL_TAB_ID, "All", "All", "", false, true);
	}

	public static String globalTabId(String category) {
		return GLOBAL_PREFIX + category;
	}

	public static String serverTabId(String category, String serverKey) {
		return SERVER_PREFIX + normalizeServerKey(serverKey) + "|" + category;
	}

	public static String currentServerKey(MinecraftClient client) {
		if (client == null) {
			return "";
		}

		ServerInfo entry = client.getCurrentServerEntry();
		if (entry != null && entry.address != null && !entry.address.isBlank()) {
			return normalizeServerKey(entry.address);
		}
		return "";
	}

	public static String currentServerLabel(MinecraftClient client) {
		if (client == null) {
			return "";
		}

		ServerInfo entry = client.getCurrentServerEntry();
		if (entry == null) {
			return "";
		}
		if (entry.name != null && !entry.name.isBlank()) {
			return entry.name;
		}
		return entry.address == null ? "" : entry.address;
	}

	public static String normalizeServerKey(String value) {
		if (value == null) {
			return "";
		}

		String normalized = value.trim().toLowerCase(Locale.ROOT);
		int slash = normalized.indexOf('/');
		if (slash >= 0) {
			normalized = normalized.substring(0, slash);
		}
		return normalized;
	}
}
