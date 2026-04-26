package com.kuzay2023.client.fakescoreboard;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MoneyParser {
	private static final String USERNAME = "[.A-Za-z0-9_]+";
	private static final String AMOUNT = "\\$?([0-9][0-9,]*(?:\\.[0-9]+)?(?:[mMbB])?)";
	private static final Pattern[] RECEIVED_PATTERNS = new Pattern[] {
		Pattern.compile("(?i)^(" + USERNAME + ")\\s+(?:paid you|has paid you|sent you)\\s+" + AMOUNT + ".*$"),
		Pattern.compile("(?i)^you received\\s+" + AMOUNT + "\\s+from\\s+(" + USERNAME + ").*$"),
		Pattern.compile("(?i)^received\\s+" + AMOUNT + "\\s+from\\s+(" + USERNAME + ").*$")
	};
	private static final Pattern[] SENT_PATTERNS = new Pattern[] {
		Pattern.compile("(?i)^you\\s+(?:paid|sent)\\s+(" + USERNAME + ")\\s+" + AMOUNT + ".*$"),
		Pattern.compile("(?i)^paid\\s+(" + USERNAME + ")\\s+" + AMOUNT + ".*$")
	};

	private MoneyParser() {
	}

	public static Optional<Double> parseDelta(String message) {
		String clean = message.strip();
		for (Pattern pattern : RECEIVED_PATTERNS) {
			Matcher matcher = pattern.matcher(clean);
			if (matcher.matches()) {
				return Optional.of(parseAmount(lastAmountGroup(matcher)));
			}
		}

		for (Pattern pattern : SENT_PATTERNS) {
			Matcher matcher = pattern.matcher(clean);
			if (matcher.matches()) {
				return Optional.of(-parseAmount(lastAmountGroup(matcher)));
			}
		}

		return Optional.empty();
	}

	private static String lastAmountGroup(Matcher matcher) {
		return matcher.group(matcher.groupCount());
	}

	public static double parseAmount(String raw) {
		String normalized = raw.replace(",", "").trim();
		char suffix = normalized.charAt(normalized.length() - 1);
		double multiplier = 1.0D;
		if (suffix == 'm' || suffix == 'M') {
			multiplier = 1_000_000D;
			normalized = normalized.substring(0, normalized.length() - 1);
		} else if (suffix == 'b' || suffix == 'B') {
			multiplier = 1_000_000_000D;
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		if (normalized.startsWith("$")) {
			normalized = normalized.substring(1);
		}

		return Double.parseDouble(normalized) * multiplier;
	}
}
