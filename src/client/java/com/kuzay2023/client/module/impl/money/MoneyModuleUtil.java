package com.kuzay2023.client.module.impl.money;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class MoneyModuleUtil {
	private static final Pattern PRICE_LINE_PATTERN = Pattern.compile("(?i)(?:price|each|per(?:\\s+item)?|ea)\\s*:?\\s*\\$?\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*([kmb])?");
	private static final Pattern PAYOUT_LINE_PATTERN = Pattern.compile("(?i)(?:pay|payout|reward|total)\\s*:?\\s*\\$?\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*([kmb])?");
	private static final Pattern MONEY_PATTERN = Pattern.compile("\\$\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*([kmb])?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PLAYER_PATTERN = Pattern.compile("(?i)(player|from|by|seller|owner)\\s*:\\s*([a-zA-Z0-9_.]+)");

	private MoneyModuleUtil() {
	}

	public static void sendCommand(MinecraftClient client, String command) {
		if (client.player == null) {
			return;
		}

		String sanitized = command.startsWith("/") ? command.substring(1) : command;
		client.player.networkHandler.sendChatCommand(sanitized);
	}

	public static GenericContainerScreenHandler currentContainer(MinecraftClient client) {
		if (client.player == null) {
			return null;
		}

		ScreenHandler handler = client.player.currentScreenHandler;
		return handler instanceof GenericContainerScreenHandler container ? container : null;
	}

	public static boolean isContainerScreen(MinecraftClient client) {
		return client.currentScreen instanceof GenericContainerScreen;
	}

	public static int findSlot(ScreenHandler handler, Item item) {
		for (Slot slot : handler.slots) {
			if (slot.getStack().isOf(item)) {
				return slot.id;
			}
		}
		return -1;
	}

	public static int findSlot(ScreenHandler handler, Item item, int count) {
		for (Slot slot : handler.slots) {
			if (slot.getStack().isOf(item) && slot.getStack().getCount() == count) {
				return slot.id;
			}
		}
		return -1;
	}

	public static boolean hasSpace(MinecraftClient client) {
		if (client.player == null) {
			return false;
		}

		for (int index = 9; index <= 35; index++) {
			if (client.player.getInventory().getStack(index).isEmpty()) {
				return true;
			}
		}

		return false;
	}

	public static boolean inventoryContains(MinecraftClient client, Item item) {
		if (client.player == null) {
			return false;
		}

		for (ItemStack stack : client.player.getInventory().main) {
			if (stack.isOf(item)) {
				return true;
			}
		}

		return false;
	}

	public static Item resolveItem(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return Items.AIR;
		}

		String normalized = itemId.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		Identifier identifier = normalized.contains(":")
			? Identifier.tryParse(normalized)
			: Identifier.tryParse("minecraft:" + normalized);
		if (identifier == null || !Registries.ITEM.containsId(identifier)) {
			return Items.AIR;
		}
		return Registries.ITEM.get(identifier);
	}

	public static boolean isPlayerInventorySlot(MinecraftClient client, ScreenHandler handler, Slot slot) {
		if (client.player == null) {
			return false;
		}
		if (slot.inventory == client.player.getInventory()) {
			return true;
		}
		return slot.id >= Math.max(0, handler.slots.size() - 36);
	}

	public static int getContainerSlotCount(ScreenHandler handler) {
		if (handler == null) {
			return 0;
		}
		return Math.max(0, handler.slots.size() - 36);
	}

	public static Slot getContainerSlot(ScreenHandler handler, int slotIndex) {
		if (handler == null || slotIndex < 0 || slotIndex >= getContainerSlotCount(handler)) {
			return null;
		}
		return handler.slots.get(slotIndex);
	}

	public static int getOrderRefreshSlotIndex(ScreenHandler handler) {
		return handler != null && handler.slots.size() > 49 ? 49 : -1;
	}

	public static boolean fastRefreshOrders(MinecraftClient client, ScreenHandler handler) {
		if (client.player == null || client.interactionManager == null || handler == null) {
			return false;
		}

		int refreshSlot = getOrderRefreshSlotIndex(handler);
		if (refreshSlot < 0) {
			return false;
		}

		for (int click = 0; click < 3; click++) {
			client.interactionManager.clickSlot(handler.syncId, refreshSlot, 1, SlotActionType.QUICK_MOVE, client.player);
		}
		return true;
	}

	public static double parsePrice(String rawPrice) {
		try {
			String cleaned = rawPrice.toLowerCase(Locale.ROOT).replace(",", "").trim();
			double multiplier = 1.0;
			if (cleaned.endsWith("b")) {
				multiplier = 1_000_000_000.0;
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			} else if (cleaned.endsWith("m")) {
				multiplier = 1_000_000.0;
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			} else if (cleaned.endsWith("k")) {
				multiplier = 1_000.0;
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			}

			return Double.parseDouble(cleaned) * multiplier;
		} catch (Exception ignored) {
			return -1.0;
		}
	}

	public static double tooltipPrice(MinecraftClient client, ItemStack stack) {
		if (client.player == null || client.world == null || stack.isEmpty()) {
			return -1.0;
		}

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC);
		for (Text line : tooltip) {
			double price = parseTooltipPrice(line.getString(), PRICE_LINE_PATTERN);
			if (price >= 0.0) {
				return price;
			}
		}

		for (Text line : tooltip) {
			double price = parseTooltipPrice(line.getString(), PAYOUT_LINE_PATTERN);
			if (price >= 0.0) {
				return price;
			}
		}

		for (Text line : tooltip) {
			double price = parseTooltipPrice(line.getString(), MONEY_PATTERN);
			if (price >= 0.0) {
				return price;
			}
		}

		return -1.0;
	}

	public static String tooltipPlayer(MinecraftClient client, ItemStack stack) {
		if (client.player == null || client.world == null || stack.isEmpty()) {
			return null;
		}

		List<Text> tooltip = stack.getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC);
		for (Text line : tooltip) {
			Matcher matcher = PLAYER_PATTERN.matcher(line.getString());
			if (matcher.find()) {
				return matcher.group(2);
			}
		}
		return null;
	}

	private static double parseTooltipPrice(String rawLine, Pattern pattern) {
		Matcher matcher = pattern.matcher(rawLine);
		if (!matcher.find()) {
			return -1.0;
		}

		double base = Double.parseDouble(matcher.group(1).replace(",", ""));
		String suffix = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
		return switch (suffix) {
			case "k" -> base * 1_000.0;
			case "m" -> base * 1_000_000.0;
			case "b" -> base * 1_000_000_000.0;
			default -> base;
		};
	}
}
