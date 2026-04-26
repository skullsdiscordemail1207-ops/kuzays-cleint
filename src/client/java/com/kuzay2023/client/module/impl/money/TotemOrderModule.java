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

public class TotemOrderModule extends Module {
	private enum Stage {
		NONE,
		SHOP,
		SHOP_GEAR,
		SHOP_TOTEM,
		SHOP_CONFIRM,
		SHOP_CHECK_FULL,
		SHOP_EXIT,
		WAIT,
		ORDERS,
		ORDERS_SELECT,
		ORDERS_CONFIRM,
		ORDERS_FINAL_EXIT,
		CYCLE_PAUSE
	}

	private final NumberSetting minPrice = addSetting(new NumberSetting("min_price", "Min Price", "General", 850.0, 1.0, 5000.0, 1.0));
	private final BooleanSetting notifications = addSetting(new BooleanSetting("notifications", "Notifications", "General", true));
	private final BooleanSetting speedMode = addSetting(new BooleanSetting("speed_mode", "Speed Mode", "General", true));

	private Stage stage = Stage.NONE;
	private long stageStart;
	private long orderSearchStartedAt;
	private int finalExitCount;
	private long finalExitStart;
	private int orderSyncId = -1;

	public TotemOrderModule() {
		super("totem_order", "Totem Snipper", "Automatically buys totems and fulfills profitable totem orders.", "Money");
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
				stage = Stage.SHOP_GEAR;
				stageStart = now;
			}
			case SHOP_GEAR -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isTotem(stack)) {
							client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							stage = Stage.SHOP_TOTEM;
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
			case SHOP_TOTEM -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isTotem(stack)) {
							int clickCount = speedMode.getValue() ? 10 : 5;
							for (int i = 0; i < clickCount; i++) {
								client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							}
							stage = Stage.SHOP_CONFIRM;
							stageStart = now;
							return;
						}
					}
					if (now - stageStart > delay(speedMode.getValue() ? 500 : 1500)) {
						client.player.closeHandledScreen();
						stage = Stage.SHOP;
						stageStart = now;
					}
				}
			}
			case SHOP_CONFIRM -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					for (Slot slot : handler.slots) {
						ItemStack stack = slot.getStack();
						if (!stack.isEmpty() && isGreenGlass(stack)) {
							for (int i = 0; i < (speedMode.getValue() ? 3 : 2); i++) {
								client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
							}
							stage = Stage.SHOP_CHECK_FULL;
							stageStart = now;
							return;
						}
					}
					if (now - stageStart > delay(speedMode.getValue() ? 200 : 800)) {
						stage = Stage.SHOP_TOTEM;
						stageStart = now;
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
						stage = Stage.SHOP_TOTEM;
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
					MoneyModuleUtil.sendCommand(client, "/orders totem");
					stage = Stage.ORDERS;
					stageStart = now;
					orderSearchStartedAt = now;
				}
			}
			case ORDERS -> {
				if (client.currentScreen instanceof GenericContainerScreen screen) {
					ScreenHandler handler = screen.getScreenHandler();
					if (speedMode.getValue() && now - stageStart < 150) {
						return;
					}

					Slot topLeftOrder = MoneyModuleUtil.getContainerSlot(handler, 0);
					if (topLeftOrder != null) {
						ItemStack stack = topLeftOrder.getStack();
						if (!stack.isEmpty() && isTotem(stack)) {
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

					if (orderSearchStartedAt > 0L && now - orderSearchStartedAt > delay(speedMode.getValue() ? 3000 : 5000)) {
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
							orderSyncId = -1;
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
						if (MoneyModuleUtil.isPlayerInventorySlot(client, handler, slot) && isTotem(slot.getStack())) {
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
						orderSyncId = -1;
						stage = Stage.SHOP;
						stageStart = now;
					}
				} else if (now - stageStart > delay(speedMode.getValue() ? 1500 : 3000)) {
					orderSyncId = -1;
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
						orderSyncId = -1;
						stage = Stage.SHOP;
						stageStart = now;
					}
				} else if (now - stageStart > delay(speedMode.getValue() ? 2000 : 5000)) {
					orderSyncId = -1;
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
				if (now - stageStart >= delay(speedMode.getValue() ? 10 : 25)) {
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
		if (client.currentScreen != null && stage != Stage.NONE && now - stageStart >= 20_000) {
			client.player.closeHandledScreen();
			restartAfterDelay(2L);
			return true;
		}
		return false;
	}

	private boolean isTotem(ItemStack stack) {
		return stack.getItem() == Items.TOTEM_OF_UNDYING;
	}

	private boolean isGreenGlass(ItemStack stack) {
		return stack.getItem() == Items.LIME_STAINED_GLASS_PANE || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE;
	}

	private boolean isInventoryFull(MinecraftClient client) {
		for (int i = 9; i <= 35; i++) {
			if (client.player.getInventory().getStack(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private double getOrderPrice(MinecraftClient client, ItemStack stack) {
		Item.TooltipContext tooltipContext = Item.TooltipContext.create(client.world);
		List<Text> tooltip = stack.getTooltip(tooltipContext, client.player, TooltipType.BASIC);
		Pattern[] patterns = {
			Pattern.compile("\\$([\\d,]+(?:\\.[\\d]+)?)([kmb])?", Pattern.CASE_INSENSITIVE),
			Pattern.compile("(?i)price\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?"),
			Pattern.compile("(?i)pay\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?"),
			Pattern.compile("(?i)reward\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?")
		};

		for (Text line : tooltip) {
			String text = line.getString();
			for (Pattern pattern : patterns) {
				Matcher matcher = pattern.matcher(text);
				if (matcher.find()) {
					double base = Double.parseDouble(matcher.group(1).replace(",", ""));
					String suffix = matcher.groupCount() >= 2 && matcher.group(2) != null ? matcher.group(2).toLowerCase(Locale.ROOT) : "";
					return switch (suffix) {
						case "k" -> base * 1_000.0;
						case "m" -> base * 1_000_000.0;
						case "b" -> base * 1_000_000_000.0;
						default -> base;
					};
				}
			}
		}

		return -1.0;
	}
}
