package com.kuzay2023.client.module.impl.money;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class BlazeRodSniperModule extends Module {
	private static final long GUI_STALL_TIMEOUT_MS = 20_000L;
	private static final long FLOW_STALL_TIMEOUT_MS = 8_000L;
	private static final long RESTART_DELAY_MS = 200L;
	private static final Pattern PRICE_LINE_PATTERN = Pattern.compile("(?i)(?:price|each|per(?:\\s+item)?|ea)\\s*:?\\s*\\$?\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*([kmb])?");
	private static final Pattern PAYOUT_LINE_PATTERN = Pattern.compile("(?i)(?:pay|payout|reward|total)\\s*:?\\s*\\$?\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*([kmb])?");
	private static final Pattern MONEY_PATTERN = Pattern.compile("\\$\\s*([\\d,]+(?:\\.[\\d]+)?)\\s*([kmb])?", Pattern.CASE_INSENSITIVE);

	private enum Stage {
		NONE,
		SHOP,
		SHOP_CATEGORY,
		SHOP_ITEM,
		SHOP_GLASS_PANE,
		SHOP_BUY_ONE,
		SHOP_CHECK_FULL,
		SHOP_EXIT,
		WAIT,
		ORDERS,
		ORDERS_SELECT,
		ORDERS_CONFIRM,
		ORDERS_FINAL_EXIT,
		CYCLE_PAUSE
	}

	private final NumberSetting minPrice = addSetting(new NumberSetting("min_price", "Min Price", "General", 350.0, 1.0, 1_000_000_000.0, 1.0));
	private final BooleanSetting notifications = addSetting(new BooleanSetting("notifications", "Notifications", "General", true));
	private final BooleanSetting speedMode = addSetting(new BooleanSetting("speed_mode", "Speed Mode", "General", true));

	private Stage stage = Stage.NONE;
	private long stageStart;
	private long orderSearchStartedAt;
	private int finalExitCount;
	private long finalExitStart;
	private int orderSyncId = -1;

	public BlazeRodSniperModule() {
		super("blaze_rod_sniper", "Blaze Rod Sniper", "Automatically buys blaze rods and fulfills profitable orders.", "Money");
	}

	@Override
	public void onEnable() {
		stage = Stage.SHOP;
		stageStart = System.currentTimeMillis();
		orderSearchStartedAt = 0L;
		finalExitCount = 0;
		orderSyncId = -1;
	}

	@Override
	public void onDisable() {
		stage = Stage.NONE;
		stageStart = 0L;
		orderSearchStartedAt = 0L;
		finalExitCount = 0;
		orderSyncId = -1;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.world == null || client.interactionManager == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (shouldRestart(client, now)) {
			return;
		}

		switch (stage) {
			case SHOP -> {
				MoneyModuleUtil.sendCommand(client, "/shop");
				stage = Stage.SHOP_CATEGORY;
				stageStart = now;
			}
			case SHOP_CATEGORY -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isShopCategory(stack)) {
							client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							stage = Stage.SHOP_ITEM;
							stageStart = now;
							return;
						}
					}
					if (now - stageStart > delay(speedMode.getValue() ? 1000 : 3000)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
					}
				}
			}
			case SHOP_ITEM -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isBlazeRod(stack) && slot.inventory != client.player.getInventory()) {
							client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							stage = Stage.SHOP_GLASS_PANE;
							stageStart = now;
							return;
						}
					}
					if (now - stageStart > delay(speedMode.getValue() ? 300 : 1000)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
					}
				}
			}
			case SHOP_GLASS_PANE -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isGlassPane(stack) && stack.getCount() == 64) {
							client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							stage = Stage.SHOP_BUY_ONE;
							stageStart = now;
							return;
						}
					}
					if (now - stageStart > delay(speedMode.getValue() ? 300 : 1000)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
					}
				}
			}
			case SHOP_BUY_ONE -> {
				if (now - stageStart >= delay(speedMode.getValue() ? 500 : 1000)) {
					if (client.currentScreen instanceof GenericContainerScreen screen) {
						ScreenHandler handler = screen.getScreenHandler();
						for (Slot slot : handler.slots) {
							ItemStack stack = slot.getStack();
							if (!stack.isEmpty() && isGreenGlass(stack) && stack.getCount() == 1) {
								int maxClicks = speedMode.getValue() ? 50 : 30;
								for (int i = 0; i < maxClicks; i++) {
									client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
									if (isInventoryFull(client)) break;
								}
								stage = Stage.SHOP_CHECK_FULL;
								stageStart = now;
								return;
							}
						}
						if (now - stageStart > delay(speedMode.getValue() ? 2000 : 3000)) {
							stage = Stage.SHOP_GLASS_PANE;
							stageStart = now;
						}
					}
				}
			}
			case SHOP_CHECK_FULL -> {
				if (now - stageStart > delay(speedMode.getValue() ? 100 : 200)) {
					if (isInventoryFull(client)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP_EXIT;
						stageStart = now;
					} else if (now - stageStart > delay(speedMode.getValue() ? 200 : 400)) {
						stage = Stage.SHOP_BUY_ONE;
						stageStart = now;
					}
				}
			}
			case SHOP_EXIT -> {
				if (client.currentScreen == null) {
					stage = Stage.WAIT;
					stageStart = now;
				}
				if (now - stageStart > delay(speedMode.getValue() ? 1000 : 5000)) {
					client.player.closeHandledScreen();
					stage = Stage.SHOP;
					stageStart = now;
				}
			}
			case WAIT -> {
				if (now - stageStart >= delay(speedMode.getValue() ? 25 : 50)) {
					MoneyModuleUtil.sendCommand(client, "/orders blaze rod");
					stage = Stage.ORDERS;
					stageStart = now;
					orderSearchStartedAt = now;
				}
			}
			case ORDERS -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					if (speedMode.getValue() && now - stageStart < 150) {
						return;
					}

					ScreenHandler handler = screen.getScreenHandler();
					Slot topLeftOrder = MoneyModuleUtil.getContainerSlot(handler, 0);
					if (topLeftOrder != null) {
						ItemStack stack = topLeftOrder.getStack();
						if (!stack.isEmpty() && isBlazeRod(stack)) {
							double orderPrice = getOrderPrice(client, stack);
							if (orderPrice >= minPrice.getValue()) {
								orderSyncId = handler.syncId;
								orderSearchStartedAt = 0L;
								client.interactionManager.clickSlot(handler.syncId, topLeftOrder.id, 0, SlotActionType.PICKUP, client.player);
								stage = Stage.ORDERS_SELECT;
								stageStart = now;
								return;
							}
						}
					}

					if (now - stageStart >= delay(speedMode.getValue() ? 250 : 600) && MoneyModuleUtil.fastRefreshOrders(client, handler)) {
						stageStart = now;
						return;
					}

					if (orderSearchStartedAt > 0L && now - orderSearchStartedAt > delay(speedMode.getValue() ? 2500 : 5000)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
						orderSearchStartedAt = 0L;
					}
				}
			}
			case ORDERS_SELECT -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					if (handler.syncId == orderSyncId) {
						if (now - stageStart > delay(speedMode.getValue() ? 1500 : 3000)) {
							client.player.closeHandledScreen();
							stage = Stage.SHOP;
							stageStart = now;
						}
						return;
					}

					if (now - stageStart < delay(speedMode.getValue() ? 50 : 150)) {
						return;
					}

					boolean movedAny = false;
					for (Slot slot : handler.slots) {
						if (slot.inventory == client.player.getInventory() && isBlazeRod(slot.getStack())) {
							client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, client.player);
							movedAny = true;
						}
					}

					if (movedAny) {
						client.player.closeHandledScreen();
						stage = Stage.ORDERS_CONFIRM;
						stageStart = now;
						return;
					}

					if (now - stageStart > delay(speedMode.getValue() ? 1500 : 3000)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
					}
				} else if (now - stageStart > delay(speedMode.getValue() ? 1500 : 3000)) {
					stage = Stage.SHOP;
					stageStart = now;
				}
			}
			case ORDERS_CONFIRM -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isGreenGlass(stack)) {
							for (int i = 0; i < (speedMode.getValue() ? 15 : 5); i++) {
								client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							}
							orderSyncId = -1;
							stage = Stage.ORDERS_FINAL_EXIT;
							stageStart = now;
							finalExitCount = 0;
							finalExitStart = now;
							return;
						}
					}
					if (now - stageStart > delay(speedMode.getValue() ? 2000 : 5000)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
					}
				} else if (now - stageStart > delay(speedMode.getValue() ? 2000 : 5000)) {
					stage = Stage.SHOP;
					stageStart = now;
				}
			}
			case ORDERS_FINAL_EXIT -> {
				long exitDelay = delay(speedMode.getValue() ? 50 : 200);
				if (finalExitCount < 2) {
					if (now - finalExitStart >= exitDelay) {
						client.player.closeHandledScreen();
						finalExitCount++;
						finalExitStart = now;
					}
				} else {
					stage = Stage.CYCLE_PAUSE;
					stageStart = now;
				}
			}
			case CYCLE_PAUSE -> {
				if (now - stageStart >= delay(speedMode.getValue() ? 25 : 50)) {
					orderSyncId = -1;
					orderSearchStartedAt = 0L;
					restartAfterDelay(2L);
				}
			}
			case NONE -> {
			}
		}
	}

	private long delay(long value) {
		return value;
	}

	private boolean shouldRestart(MinecraftClient client, long now) {
		if (stage == Stage.NONE || stageStart <= 0L) {
			return false;
		}

		long elapsed = now - stageStart;
		if (client.currentScreen instanceof GenericContainerScreen && elapsed >= GUI_STALL_TIMEOUT_MS) {
			forceRestart(client);
			return true;
		}

		if (stage != Stage.SHOP && stage != Stage.CYCLE_PAUSE && elapsed >= FLOW_STALL_TIMEOUT_MS) {
			forceRestart(client);
			return true;
		}
		return false;
	}

	private void forceRestart(MinecraftClient client) {
		if (client.player != null && client.currentScreen instanceof GenericContainerScreen) {
			client.player.closeHandledScreen();
		}
		restartAfterDelay(RESTART_DELAY_MS);
	}

	private boolean isShopCategory(ItemStack stack) {
		return stack.isOf(Items.NETHERRACK) || stack.getName().getString().toLowerCase(Locale.ROOT).contains("nether");
	}

	private boolean isBlazeRod(ItemStack stack) {
		return stack.getItem() == Items.BLAZE_ROD;
	}

	private boolean isGlassPane(ItemStack stack) {
		String itemName = stack.getItem().getName().getString().toLowerCase(Locale.ROOT);
		return itemName.contains("glass") && itemName.contains("pane");
	}

	private boolean isGreenGlass(ItemStack stack) {
		return stack.isOf(Items.LIME_STAINED_GLASS_PANE) || stack.isOf(Items.GREEN_STAINED_GLASS_PANE);
	}

	private boolean isInventoryFull(MinecraftClient client) {
		for (int i = 9; i < 36; i++) {
			if (client.player.getInventory().getStack(i).isEmpty()) return false;
		}
		return true;
	}

	private double getOrderPrice(MinecraftClient client, ItemStack stack) {
		Item.TooltipContext tooltipContext = Item.TooltipContext.create(client.world);
		List<Text> tooltip = stack.getTooltip(tooltipContext, client.player, TooltipType.BASIC);
		for (Text line : tooltip) {
			double price = parsePrice(line.getString(), PRICE_LINE_PATTERN);
			if (price >= 0.0) {
				return price;
			}
		}

		for (Text line : tooltip) {
			double price = parsePrice(line.getString(), PAYOUT_LINE_PATTERN);
			if (price >= 0.0) {
				return price;
			}
		}

		for (Text line : tooltip) {
			double price = parsePrice(line.getString(), MONEY_PATTERN);
			if (price >= 0.0) {
				return price;
			}
		}

		return -1.0;
	}

	private double parsePrice(String rawLine, Pattern pattern) {
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
