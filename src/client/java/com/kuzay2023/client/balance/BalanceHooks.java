package com.kuzay2023.client.balance;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardClientHooks;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardConfig;
import com.kuzay2023.client.fakescoreboard.MoneyParser;
import com.kuzay2023.client.module.Module;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class BalanceHooks {
	private static final Pattern BALANCE_PATTERN = Pattern.compile("(?i)(?:(?:balance|bal)\\b|you\\s+have\\b)[^\\d$-]*([$]?[0-9][0-9,]*(?:\\.[0-9]+)?(?:[mMbB])?)");

	private static boolean initialized;
	private static double realBalance = Double.NaN;
	private static long pendingFakeBalanceUntil;
	private static long silentBalanceRequestUntil;
	private static int scheduledSilentBalanceTicks;

	private BalanceHooks() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			String normalized = command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
			if (normalized.equals("bal") || normalized.equals("balance")) {
				if (isSilentBalanceRequestActive()) {
					pendingFakeBalanceUntil = 0L;
					return true;
				}
				if (sendLocalFakeBalance()) {
					return false;
				}
				pendingFakeBalanceUntil = System.currentTimeMillis() + 5_000L;
			}
			return true;
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> captureRealBalance(message));
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> captureRealBalance(message));
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !shouldSuppressSilentBalanceMessage(message));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> !shouldSuppressSilentBalanceMessage(message));
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> spoofIfNeeded(message));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			Text updated = spoofIfNeeded(message);
			if (updated == message) {
				return true;
			}

			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) {
				client.inGameHud.getChatHud().addMessage(updated);
			}
			return false;
		});
	}

	public static double getRealBalance() {
		return realBalance;
	}

	public static void requestSilentRealBalance(MinecraftClient client) {
		if (client == null || client.player == null) {
			return;
		}

		scheduledSilentBalanceTicks = 10;
	}

	public static void tick(MinecraftClient client) {
		if (scheduledSilentBalanceTicks <= 0) {
			return;
		}
		if (client == null || client.player == null || client.getNetworkHandler() == null) {
			return;
		}

		scheduledSilentBalanceTicks--;
		if (scheduledSilentBalanceTicks > 0) {
			return;
		}

		silentBalanceRequestUntil = System.currentTimeMillis() + 5_000L;
		pendingFakeBalanceUntil = 0L;
		client.player.networkHandler.sendChatCommand("bal");
	}

	public static String getFormattedRealBalance() {
		if (Double.isNaN(realBalance)) {
			return "Unknown";
		}
		return FakeScoreboardConfig.formatMoney(realBalance);
	}

	private static void captureRealBalance(Text message) {
		String raw = message.getString();
		if (isSilentBalanceRequestActive()) {
			findBalanceValue(raw).ifFound(value -> {
				realBalance = value;
				silentBalanceRequestUntil = 0L;
				pendingFakeBalanceUntil = 0L;
			});
			return;
		}

		if (System.currentTimeMillis() <= pendingFakeBalanceUntil) {
			return;
		}

		if (!isFakeBalanceEnabled()) {
			findBalanceValue(raw).ifFound(value -> realBalance = value);
		}

		MoneyParser.parseDelta(raw).ifPresent(delta -> {
			if (!Double.isNaN(realBalance)) {
				realBalance += delta;
			}
		});
	}

	private static Text spoofIfNeeded(Text message) {
		if (!isFakeBalanceEnabled() || System.currentTimeMillis() > pendingFakeBalanceUntil || isSilentBalanceRequestActive()) {
			return message;
		}

		String raw = message.getString();
		BalanceMatch match = findBalanceValue(raw);
		if (!match.found()) {
			return message;
		}

		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		if (config == null) {
			return message;
		}

		pendingFakeBalanceUntil = 0L;
		String fakeAmount = FakeScoreboardConfig.formatMoney(config.getCurrentMoney());
		MutableText rebuilt = Text.literal(raw.substring(0, match.start())).setStyle(message.getStyle());
		rebuilt.append(Text.literal(fakeAmount).styled(style -> style.withColor(0x55FF55)));
		rebuilt.append(Text.literal(raw.substring(match.end())).setStyle(message.getStyle()));
		return rebuilt;
	}

	private static boolean isFakeBalanceEnabled() {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return false;
		}

		Module module = context.moduleManager().getModule("fake_balance");
		return module != null && module.isEnabled();
	}

	private static boolean sendLocalFakeBalance() {
		if (!isFakeBalanceEnabled()) {
			return false;
		}

		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		MinecraftClient client = MinecraftClient.getInstance();
		if (config == null || client == null || client.inGameHud == null) {
			return false;
		}

		pendingFakeBalanceUntil = 0L;
		MutableText message = Text.literal("Balance: ");
		message.append(Text.literal(FakeScoreboardConfig.formatMoney(config.getCurrentMoney())).styled(style -> style.withColor(0x55FF55)));
		client.inGameHud.getChatHud().addMessage(message);
		return true;
	}

	private static boolean isSilentBalanceRequestActive() {
		return System.currentTimeMillis() <= silentBalanceRequestUntil;
	}

	private static boolean shouldSuppressSilentBalanceMessage(Text message) {
		if (!isSilentBalanceRequestActive()) {
			return false;
		}

		BalanceMatch match = findBalanceValue(message.getString());
		if (!match.found()) {
			return false;
		}

		realBalance = match.amount();
		silentBalanceRequestUntil = 0L;
		pendingFakeBalanceUntil = 0L;
		return true;
	}

	private static BalanceMatch findBalanceValue(String text) {
		if (text == null) {
			return BalanceMatch.notFound();
		}

		Matcher matcher = BALANCE_PATTERN.matcher(text);
		if (!matcher.find()) {
			return BalanceMatch.notFound();
		}

		String rawAmount = matcher.group(1);
		try {
			double parsedAmount = MoneyParser.parseAmount(rawAmount);
			return new BalanceMatch(true, parsedAmount, matcher.start(1), matcher.end(1));
		} catch (RuntimeException ignored) {
			return BalanceMatch.notFound();
		}
	}

	private record BalanceMatch(boolean found, double amount, int start, int end) {
		private static BalanceMatch notFound() {
			return new BalanceMatch(false, 0.0D, -1, -1);
		}

		private void ifFound(java.util.function.DoubleConsumer consumer) {
			if (found) {
				consumer.accept(amount);
			}
		}
	}
}
