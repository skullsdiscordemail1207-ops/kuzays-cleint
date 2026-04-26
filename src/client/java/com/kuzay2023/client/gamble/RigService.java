package com.kuzay2023.client.gamble;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class RigService {
	public enum WinnerSide {
		WHITE,
		YELLOW;

		public WinnerSide opposite() {
			return this == WHITE ? YELLOW : WHITE;
		}
	}

	private static final Pattern DIGIT_PATTERN = Pattern.compile("([1-9])");

	private static WinnerSide winnerSide = WinnerSide.WHITE;
	private static int winningNumber = 9;
	private static int losingNumber = 4;
	private static boolean rigEnabled;
	private static boolean hudVisible = true;
	private static boolean awaitingLeverRound;
	private static boolean namesActive;
	private static int baselineWhiteCount;
	private static int baselineYellowCount;
	private static Item playerAItem = Items.WHITE_CANDLE;
	private static Item playerBItem = Items.CANDLE;
	private static boolean useKeyWasPressed;

	private RigService() {
	}

	public static void setRigEnabled(boolean value) {
		rigEnabled = value;
		if (!value) {
			clearActiveNames();
		}
	}

	public static boolean isRigEnabled() {
		return rigEnabled;
	}

	public static void setHudVisible(boolean value) {
		hudVisible = value;
	}

	public static boolean isHudVisible() {
		return hudVisible;
	}

	public static void setWinnerSide(WinnerSide side) {
		if (side != null && winnerSide != side) {
			winnerSide = side;
			clearActiveNames();
		}
	}

	public static WinnerSide getWinnerSide() {
		return winnerSide;
	}

	public static void setPlayerAItem(Item item) {
		if (item != null && item != Items.AIR && item != playerAItem) {
			playerAItem = item;
			clearActiveNames();
		}
	}

	public static void setPlayerBItem(Item item) {
		if (item != null && item != Items.AIR && item != playerBItem) {
			playerBItem = item;
			clearActiveNames();
		}
	}

	public static Item getWinnerItem() {
		return winnerSide == WinnerSide.WHITE ? playerAItem : playerBItem;
	}

	public static Item getLoserItem() {
		return winnerSide == WinnerSide.WHITE ? playerBItem : playerAItem;
	}

	public static boolean isRiggedCandle(Item item) {
		return item == playerAItem || item == playerBItem;
	}

	public static boolean hasActiveNames() {
		return namesActive;
	}

	public static int getDisplayedNumber(ItemStack stack) {
		return stack.isOf(getWinnerItem()) ? winningNumber : losingNumber;
	}

	public static void tick(MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		if (!rigEnabled) {
			useKeyWasPressed = client.options.useKey.isPressed();
			return;
		}

		InventorySnapshot snapshot = scanInventory(client);
		updateInventoryState(snapshot.whiteCount, snapshot.yellowCount, snapshot.whiteNumber, snapshot.yellowNumber);
		detectLeverUse(client, snapshot);
	}

	private static void detectLeverUse(MinecraftClient client, InventorySnapshot snapshot) {
		boolean usePressed = client.options.useKey.isPressed();
		if (usePressed && !useKeyWasPressed && isLookingAtLever(client)) {
			armFromLever(snapshot.whiteCount, snapshot.yellowCount);
		}
		useKeyWasPressed = usePressed;
	}

	private static boolean isLookingAtLever(MinecraftClient client) {
		if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
			return false;
		}

		BlockHitResult blockHitResult = (BlockHitResult) client.crosshairTarget;
		BlockState blockState = client.world.getBlockState(blockHitResult.getBlockPos());
		return blockState.isOf(Blocks.LEVER);
	}

	private static InventorySnapshot scanInventory(MinecraftClient client) {
		InventorySnapshot snapshot = new InventorySnapshot();
		for (ItemStack stack : client.player.getInventory().main) {
			collectStackData(snapshot, stack);
		}
		for (ItemStack stack : client.player.getInventory().offHand) {
			collectStackData(snapshot, stack);
		}
		return snapshot;
	}

	private static void collectStackData(InventorySnapshot snapshot, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		if (stack.isOf(playerAItem)) {
			snapshot.whiteCount += stack.getCount();
			snapshot.whiteNumber = chooseParsedNumber(snapshot.whiteNumber, parseNumber(stack));
		} else if (stack.isOf(playerBItem)) {
			snapshot.yellowCount += stack.getCount();
			snapshot.yellowNumber = chooseParsedNumber(snapshot.yellowNumber, parseNumber(stack));
		}
	}

	private static Integer chooseParsedNumber(Integer current, Integer candidate) {
		if (candidate == null) return current;
		if (current == null) return candidate;
		return candidate;
	}

	private static Integer parseNumber(ItemStack stack) {
		String name = stack.getName().getString();
		Matcher matcher = DIGIT_PATTERN.matcher(name);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}

		String lowered = name.toLowerCase(Locale.ROOT);
		if (lowered.contains("one")) return 1;
		if (lowered.contains("two")) return 2;
		if (lowered.contains("three")) return 3;
		if (lowered.contains("four")) return 4;
		if (lowered.contains("five")) return 5;
		if (lowered.contains("six")) return 6;
		if (lowered.contains("seven")) return 7;
		if (lowered.contains("eight")) return 8;
		if (lowered.contains("nine")) return 9;
		return null;
	}

	private static void clearActiveNames() {
		awaitingLeverRound = false;
		namesActive = false;
	}

	private static void armFromLever(int whiteCount, int yellowCount) {
		baselineWhiteCount = whiteCount;
		baselineYellowCount = yellowCount;
		awaitingLeverRound = true;
		namesActive = false;
	}

	private static void updateInventoryState(int whiteCount, int yellowCount, Integer whiteNameNumber, Integer yellowNameNumber) {
		boolean gainedWhite = whiteCount > baselineWhiteCount;
		boolean gainedYellow = yellowCount > baselineYellowCount;
		boolean hasBothColorsNow = whiteCount > 0 && yellowCount > 0;
		boolean sawAnyNewCandle = gainedWhite || gainedYellow;

		if (awaitingLeverRound && hasBothColorsNow && sawAnyNewCandle) {
			int otherSideNumber = winnerSide == WinnerSide.WHITE ? fallbackNumber(yellowNameNumber) : fallbackNumber(whiteNameNumber);
			rerollNumbers(otherSideNumber);
			namesActive = true;
			awaitingLeverRound = false;
			return;
		}

		if (namesActive && whiteCount == 0 && yellowCount == 0) {
			clearActiveNames();
		}
	}

	private static int fallbackNumber(Integer value) {
		return value != null ? Math.max(1, Math.min(9, value)) : ThreadLocalRandom.current().nextInt(1, 10);
	}

	private static void rerollNumbers(int otherSideNumber) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		losingNumber = Math.max(1, Math.min(9, otherSideNumber));

		if (winnerSide == WinnerSide.YELLOW) {
			losingNumber = Math.min(losingNumber, 8);
			winningNumber = random.nextInt(losingNumber + 1, 10);
			return;
		}

		winningNumber = random.nextInt(losingNumber, 10);
	}

	private static final class InventorySnapshot {
		private int whiteCount;
		private int yellowCount;
		private Integer whiteNumber;
		private Integer yellowNumber;
	}
}
