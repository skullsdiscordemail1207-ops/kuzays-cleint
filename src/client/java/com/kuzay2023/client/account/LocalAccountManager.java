package com.kuzay2023.client.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.config.LocalAccountConfig;
import com.kuzay2023.client.mixin.client.MinecraftClientSessionAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

public final class LocalAccountManager {
	private LocalAccountManager() {
	}

	public static List<LocalAccountConfig> accounts() {
		KuzayClientContext context = KuzayClientModClient.getContext();
		return context == null ? List.of() : context.configManager().getConfig().accounts;
	}

	public static void save(KuzayClientContext context) {
		if (context != null) {
			context.configManager().save(context);
		}
	}

	public static LocalAccountConfig fromCurrentSession(MinecraftClient client) {
		LocalAccountConfig config = new LocalAccountConfig();
		if (client == null) {
			return config;
		}
		Session session = client.getSession();
		config.label = session.getUsername();
		config.username = session.getUsername();
		config.uuid = session.getUuidOrNull() == null ? "" : session.getUuidOrNull().toString();
		config.accessToken = session.getAccessToken();
		config.clientId = session.getClientId().orElse("");
		config.xuid = session.getXuid().orElse("");
		config.accountType = session.getAccountType().getName();
		return config;
	}

	public static boolean applyAccount(MinecraftClient client, LocalAccountConfig config) {
		if (client == null || config == null || config.username == null || config.username.isBlank()) {
			return false;
		}

		UUID uuid = parseUuid(config.uuid).orElseGet(() -> UUID.nameUUIDFromBytes(config.username.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		Session session = new Session(
			config.username,
			uuid,
			config.accessToken == null || config.accessToken.isBlank() ? "0" : config.accessToken,
			optional(config.clientId),
			optional(config.xuid),
			Session.AccountType.byName(config.accountType == null || config.accountType.isBlank() ? "msa" : config.accountType)
		);
		((MinecraftClientSessionAccessor) client).kuzay$setSession(session);
		return true;
	}

	private static Optional<UUID> parseUuid(String raw) {
		try {
			return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(raw));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	private static Optional<String> optional(String raw) {
		return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(raw);
	}
}
