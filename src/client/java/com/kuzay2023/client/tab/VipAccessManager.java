package com.kuzay2023.client.tab;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.config.ClientConfig;
import com.kuzay2023.client.tab.VipApiClient.VipCheckResponse;
import com.kuzay2023.client.tab.VipApiClient.VipClaimResponse;

import net.minecraft.client.MinecraftClient;

public final class VipAccessManager {
	public static final String ADMIN_PASSWORD = "LIP-SINGING-COOKIE";
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
		private final AtomicInteger threadCounter = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "kuzays-secret-vip-" + threadCounter.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		}
	});

	private static boolean unlocked;
	private static long unlockExpiresAt = -1L;
	private static String unlockDisplay = "";
	private static String clientId = "";
	private static String sessionToken = "";
	private static long nextValidationAt;
	private static boolean validationInFlight;
	private static KuzayClientContext context;

	private VipAccessManager() {
	}

	public static void initialize(KuzayClientContext newContext) {
		context = newContext;
		if (context == null) {
			return;
		}

		ClientConfig config = context.configManager().getConfig();
		if (config.vipClientId == null || config.vipClientId.isBlank()) {
			config.vipClientId = UUID.randomUUID().toString();
		}
		clientId = config.vipClientId;
		if (config.vipRegistryUrl == null || config.vipRegistryUrl.isBlank()) {
			config.vipRegistryUrl = "http://127.0.0.1:8787";
		}
		nextValidationAt = 0L;
		context.configManager().save(context);
	}

	public static boolean isUnlocked() {
		if (unlockExpiresAt > 0L && System.currentTimeMillis() > unlockExpiresAt) {
			clear();
		}
		return unlocked;
	}

	public static CompletableFuture<VipUnlockResult> tryUnlockAsync(String password) {
		String trimmed = password == null ? "" : password.trim();
		if (trimmed.isBlank()) {
			return CompletableFuture.completedFuture(new VipUnlockResult(false, "Enter a VIP password."));
		}

		if (ADMIN_PASSWORD.equals(trimmed)) {
			unlocked = true;
			unlockExpiresAt = -1L;
			unlockDisplay = "lifetime";
			sessionToken = "";
			nextValidationAt = 0L;
			persist();
			return CompletableFuture.completedFuture(new VipUnlockResult(true, "VIP unlocked."));
		}

		if (context == null) {
			return CompletableFuture.completedFuture(new VipUnlockResult(false, "Client context is not ready yet."));
		}

		ClientConfig config = context.configManager().getConfig();
		String registryUrl = config.vipRegistryUrl == null ? "" : config.vipRegistryUrl.trim();
		if (registryUrl.isBlank()) {
			return CompletableFuture.completedFuture(new VipUnlockResult(false, "Set vipRegistryUrl in the client config first."));
		}

		String requestClientId = ensureClientId();
		String username = MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getSession() != null
			? MinecraftClient.getInstance().getSession().getUsername()
			: "unknown";

		return CompletableFuture.supplyAsync(() -> {
			try {
				VipClaimResponse response = VipApiClient.claim(registryUrl, trimmed, requestClientId, username);
				if (!response.ok()) {
					return new VipUnlockResult(false, response.message() == null || response.message().isBlank() ? "VIP key rejected." : response.message());
				}

				applyGrant(response.sessionToken(), response.expiresAt(), response.display(), response.lifetime());
				persist();
				return new VipUnlockResult(true, response.message() == null || response.message().isBlank() ? "VIP unlocked." : response.message());
			} catch (IOException | InterruptedException exception) {
				if (exception instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				throw new CompletionException(exception);
			}
		}, EXECUTOR);
	}

	public static boolean tryUnlock(String password) {
		if (ADMIN_PASSWORD.equals(password)) {
			unlocked = true;
			unlockExpiresAt = -1L;
			unlockDisplay = "lifetime";
		}
		return unlocked;
	}

	public static void loadFromConfig(ClientConfig config) {
		if (config == null) {
			clear();
			return;
		}
		unlocked = config.vipUnlocked;
		unlockExpiresAt = config.vipUnlockExpiresAt;
		unlockDisplay = config.vipUnlockDisplay == null ? "" : config.vipUnlockDisplay;
		clientId = config.vipClientId == null ? "" : config.vipClientId;
		sessionToken = config.vipSessionToken == null ? "" : config.vipSessionToken;
		nextValidationAt = 0L;
		if (unlockExpiresAt > 0L && System.currentTimeMillis() > unlockExpiresAt) {
			clear();
		}
	}

	public static void applyToConfig(ClientConfig config) {
		if (config == null) {
			return;
		}
		config.vipUnlocked = isUnlocked();
		config.vipUnlockExpiresAt = unlockExpiresAt;
		config.vipUnlockDisplay = unlockDisplay;
		config.vipClientId = clientId;
		config.vipSessionToken = sessionToken;
	}

	public static String getStatusText() {
		if (!isUnlocked()) {
			return "VIP: locked";
		}
		if (unlockExpiresAt <= 0L) {
			return "VIP: lifetime";
		}
		long remainingMs = Math.max(0L, unlockExpiresAt - System.currentTimeMillis());
		long totalMinutes = remainingMs / 60000L;
		long days = totalMinutes / (60L * 24L);
		long hours = (totalMinutes / 60L) % 24L;
		long minutes = totalMinutes % 60L;
		return "VIP: " + days + "d " + hours + "h " + minutes + "m";
	}

	public static void clear() {
		unlocked = false;
		unlockExpiresAt = -1L;
		unlockDisplay = "";
		sessionToken = "";
		nextValidationAt = 0L;
		validationInFlight = false;
	}

	public static void tick(MinecraftClient client, KuzayClientContext activeContext) {
		if (activeContext != null && context == null) {
			initialize(activeContext);
		}
		if (context == null) {
			return;
		}
		if (validationInFlight) {
			return;
		}

		ClientConfig config = context.configManager().getConfig();
		if (config.vipRegistryUrl == null || config.vipRegistryUrl.isBlank() || sessionToken == null || sessionToken.isBlank()) {
			return;
		}

		long now = System.currentTimeMillis();
		if (nextValidationAt > now) {
			return;
		}

		validationInFlight = true;
		String registryUrl = config.vipRegistryUrl.trim();
		String requestClientId = ensureClientId();
		CompletableFuture
			.supplyAsync(() -> {
				try {
					return VipApiClient.check(registryUrl, sessionToken, requestClientId);
				} catch (IOException | InterruptedException exception) {
					if (exception instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					throw new CompletionException(exception);
				}
			}, EXECUTOR)
			.whenComplete((response, throwable) -> {
				validationInFlight = false;
				if (throwable != null) {
					nextValidationAt = System.currentTimeMillis() + 60_000L;
					return;
				}

				VipCheckResponse checkResponse = response;
				if (checkResponse == null || !checkResponse.ok() || !checkResponse.valid()) {
					clear();
					persist();
					return;
				}

				applyGrant(sessionToken, checkResponse.expiresAt(), checkResponse.display(), checkResponse.lifetime());
				nextValidationAt = System.currentTimeMillis() + 300_000L;
				persist();
			});
	}

	private static void applyGrant(String newSessionToken, long expiresAt, String display, boolean lifetime) {
		unlocked = true;
		sessionToken = newSessionToken == null ? "" : newSessionToken;
		unlockExpiresAt = lifetime ? -1L : expiresAt;
		if (lifetime) {
			unlockDisplay = "lifetime";
		} else if (display != null && !display.isBlank()) {
			unlockDisplay = display;
		} else {
			unlockDisplay = "";
		}
		nextValidationAt = System.currentTimeMillis() + 300_000L;
	}

	private static String ensureClientId() {
		if (clientId == null || clientId.isBlank()) {
			clientId = UUID.randomUUID().toString();
		}
		if (context != null) {
			ClientConfig config = context.configManager().getConfig();
			config.vipClientId = clientId;
		}
		return clientId;
	}

	private static void persist() {
		if (context != null) {
			context.configManager().save(context);
		}
	}
}
