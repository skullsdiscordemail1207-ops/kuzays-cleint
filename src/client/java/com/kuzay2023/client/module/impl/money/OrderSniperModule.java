package com.kuzay2023.client.module.impl.money;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class OrderSniperModule extends Module {
	private enum Stage {
		OPEN_ORDERS,
		WAIT_ORDERS_GUI,
		CLICK_SLOT_51,
		WAIT_SECOND_GUI,
		CLICK_TARGET_ITEM,
		WAIT_THIRD_GUI,
		CLICK_SLOT_13,
		WAIT_ITEMS_GUI,
		CLICK_SLOT_52,
		CLICK_SLOT_53,
		CYCLE_PAUSE
	}

	private final StringSetting targetItemId = addSetting(new StringSetting("target_item_id", "Target Item", "Target", "minecraft:diamond"));
	private final NumberSetting delayMs = addSetting(new NumberSetting("delay_ms", "Action Delay (ms)", "General", 300.0, 100.0, 2000.0, 50.0));

	private Stage stage = Stage.OPEN_ORDERS;
	private long stageMillis;
	private boolean foundTargetItem;

	public OrderSniperModule() {
		super("order_sniper", "Order Dropper", "Runs the order dropper flow for the selected item.", "Money");
	}

	@Override
	protected void onEnable() {
		stage = Stage.OPEN_ORDERS;
		stageMillis = System.currentTimeMillis();
		foundTargetItem = false;
	}

	@Override
	protected void onDisable() {
		stage = Stage.OPEN_ORDERS;
		stageMillis = 0L;
		foundTargetItem = false;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.world == null || client.interactionManager == null) {
			return;
		}

		Item targetItem = MoneyModuleUtil.resolveItem(targetItemId.getValue());
		if (targetItem == Items.AIR) {
			return;
		}

		long now = System.currentTimeMillis();
		if (shouldRestart(client, now)) {
			return;
		}

		switch (stage) {
			case OPEN_ORDERS -> {
				MoneyModuleUtil.sendCommand(client, "/order");
				next(Stage.WAIT_ORDERS_GUI, now);
			}
			case WAIT_ORDERS_GUI -> {
				if (!hasDelayElapsed(now)) {
					return;
				}
				if (client.currentScreen instanceof GenericContainerScreen) {
					next(Stage.CLICK_SLOT_51, now);
				} else if (now - stageMillis > 3000L) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case CLICK_SLOT_51 -> {
				if (!hasDelayElapsed(now) || !(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				if (handler.slots.size() > 51) {
					client.interactionManager.clickSlot(handler.syncId, 51, 0, SlotActionType.PICKUP, client.player);
					next(Stage.WAIT_SECOND_GUI, now);
				}
			}
			case WAIT_SECOND_GUI -> {
				if (!hasDelayElapsed(now)) {
					return;
				}
				if (client.currentScreen instanceof GenericContainerScreen) {
					next(Stage.CLICK_TARGET_ITEM, now);
				} else if (now - stageMillis > 3000L) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case CLICK_TARGET_ITEM -> {
				if (!hasDelayElapsed(now) || !(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				foundTargetItem = false;
				for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
					Slot slot = handler.slots.get(slotIndex);
					if (!slot.getStack().isEmpty() && slot.getStack().isOf(targetItem)) {
						client.interactionManager.clickSlot(handler.syncId, slotIndex, 0, SlotActionType.PICKUP, client.player);
						next(Stage.WAIT_THIRD_GUI, now);
						foundTargetItem = true;
						break;
					}
				}
				if (!foundTargetItem && now - stageMillis > 4000L) {
					next(Stage.CYCLE_PAUSE, now);
				}
			}
			case WAIT_THIRD_GUI -> {
				if (!hasDelayElapsed(now)) {
					return;
				}
				if (client.currentScreen instanceof GenericContainerScreen) {
					next(Stage.CLICK_SLOT_13, now);
				} else if (now - stageMillis > 3000L) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case CLICK_SLOT_13 -> {
				if (!hasDelayElapsed(now) || !(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				if (handler.slots.size() > 13) {
					Slot slot13 = handler.slots.get(13);
					if (!slot13.getStack().isEmpty() && slot13.getStack().isOf(Items.CHEST)) {
						client.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, client.player);
					} else if (handler.slots.size() > 15) {
						client.interactionManager.clickSlot(handler.syncId, 15, 0, SlotActionType.PICKUP, client.player);
					}
					next(Stage.WAIT_ITEMS_GUI, now);
				}
			}
			case WAIT_ITEMS_GUI -> {
				if (!hasDelayElapsed(now)) {
					return;
				}
				if (client.currentScreen instanceof GenericContainerScreen) {
					next(Stage.CLICK_SLOT_52, now);
				} else if (now - stageMillis > 3000L) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case CLICK_SLOT_52 -> {
				if (!hasDelayElapsed(now) || !(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				if (handler.slots.size() > 52) {
					client.interactionManager.clickSlot(handler.syncId, 52, 0, SlotActionType.PICKUP, client.player);
					next(Stage.CLICK_SLOT_53, now);
				}
			}
			case CLICK_SLOT_53 -> {
				if (!hasDelayElapsed(now)) {
					return;
				}
				if (!(client.currentScreen instanceof GenericContainerScreen screen)) {
					next(Stage.CYCLE_PAUSE, now);
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				if (handler.slots.size() <= 53) {
					return;
				}
				Slot nextPageSlot = handler.slots.get(53);
				if (!nextPageSlot.getStack().isEmpty() && nextPageSlot.getStack().isOf(Items.ARROW)) {
					client.interactionManager.clickSlot(handler.syncId, 53, 0, SlotActionType.PICKUP, client.player);
					next(Stage.CLICK_SLOT_52, now);
				} else {
					if (handler.slots.size() > 52) {
						client.interactionManager.clickSlot(handler.syncId, 52, 0, SlotActionType.PICKUP, client.player);
					}
					if (client.currentScreen instanceof GenericContainerScreen) {
						client.player.closeHandledScreen();
					}
					next(Stage.CYCLE_PAUSE, now);
				}
			}
			case CYCLE_PAUSE -> {
				if (now - stageMillis > Math.max(180L, actionDelay())) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			default -> {
			}
		}
	}

	private boolean hasDelayElapsed(long now) {
		return now - stageMillis >= actionDelay();
	}

	private long actionDelay() {
		return Math.max(100L, delayMs.getValue().longValue() - 50L);
	}

	private boolean shouldRestart(MinecraftClient client, long now) {
		if (stageMillis <= 0L) {
			return false;
		}
		if (client.currentScreen instanceof GenericContainerScreen && now - stageMillis >= 20_000L) {
			forceRestart(client);
			return true;
		}
		if (stage != Stage.OPEN_ORDERS && stage != Stage.CYCLE_PAUSE && now - stageMillis >= 8_000L) {
			forceRestart(client);
			return true;
		}
		return false;
	}

	private void forceRestart(MinecraftClient client) {
		if (client.player != null && client.currentScreen instanceof GenericContainerScreen) {
			client.player.closeHandledScreen();
		}
		restartAfterDelay(200L);
	}

	private void next(Stage next, long now) {
		stage = next;
		stageMillis = now;
	}
}
