package com.kuzay2023.client.gamble;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PaymentEntry(String payerName, String amountText) {
	public String buildPayCommand() {
		return "/pay " + payerName + " " + getTripledAmountText() + " ";
	}

	public String getTripledAmountText() {
		if (amountText == null || amountText.isBlank()) {
			return amountText;
		}

		char lastChar = amountText.charAt(amountText.length() - 1);
		boolean hasSuffix = Character.isLetter(lastChar);
		String numericPart = hasSuffix ? amountText.substring(0, amountText.length() - 1) : amountText;
		BigDecimal tripledAmount = new BigDecimal(numericPart).multiply(BigDecimal.valueOf(3));
		String formattedAmount = tripledAmount.stripTrailingZeros().toPlainString();

		if (formattedAmount.contains(".")) {
			formattedAmount = tripledAmount.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		}

		return hasSuffix ? formattedAmount + lastChar : formattedAmount;
	}
}
