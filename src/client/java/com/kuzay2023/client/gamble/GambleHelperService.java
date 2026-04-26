package com.kuzay2023.client.gamble;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class GambleHelperService {
	private static final Pattern PAYMENT_PATTERN = Pattern.compile("(?i).*?(\\.?[A-Za-z0-9_]{3,16})\\s+paid\\s+you\\s+\\$?([0-9][0-9,]*(?:\\.[0-9]{1,2})?[kmbt]?)(?:\\.|!+)?\\s*$");
	private static boolean enabled;

	private GambleHelperService() {
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static Text decoratePaymentMessage(Text message) {
		if (!enabled) {
			return message;
		}

		String username = extractUsername(message.getString());
		if (username == null) {
			return message;
		}

		return makeMessageClickable(message, username).append(createPayBackButton(username));
	}

	private static String extractUsername(String rawMessage) {
		Matcher matcher = PAYMENT_PATTERN.matcher(rawMessage);
		return matcher.matches() ? matcher.group(1) : null;
	}

	private static MutableText makeMessageClickable(Text originalMessage, String username) {
		String suggestedCommand = "/pay " + username + " ";
		Style clickableStyle = Style.EMPTY.withColor(Formatting.WHITE).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand));
		return Text.empty().setStyle(clickableStyle).append(originalMessage.copy());
	}

	private static Text createPayBackButton(String username) {
		String suggestedCommand = "/pay " + username + " ";
		return Text.literal(" [Pay Back]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand)));
	}
}
