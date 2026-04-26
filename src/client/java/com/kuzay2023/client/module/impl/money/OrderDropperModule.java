package com.kuzay2023.client.module.impl.money;

import java.util.Locale;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class OrderDropperModule extends Module {
	private enum Stage {
		OPEN_ORDERS,
		WAIT_ORDERS_GUI,
		SELECT_ORDER,
		WAIT_DEPOSIT_GUI,
		TRANSFER_ITEMS,
		WAIT_CONFIRM_GUI,
		CONFIRM_SALE,
		FINAL_EXIT,
		CYCLE_PAUSE
	}

	private final StringSetting itemName = addSetting(new StringSetting("item_name", "Item Name", "Target", "diamond"));
	private final StringSetting targetItemId = addSetting(new StringSetting("target_item_id", "Snipping Item", "Target", "minecraft:diamond"));
	private final NumberSetting minPrice = addSetting(new NumberSetting("min_price", "Sell Price", "Target", 1.0, 0.0, 1_000_000_000.0, 1.0));
	private final BooleanSetting shulkerSupport = addSetting(new BooleanSetting("shulker_support", "Shulker Support", "General", false));
	private final NumberSetting delayTicks = addSetting(new NumberSetting("delay_ticks", "Refresh Delay", "General", 2.0, 0.0, 20.0, 1.0));

	private Stage stage = Stage.OPEN_ORDERS;
	private long stageMillis;
	private int stageTicks;
	private int savedSyncId = -1;
	private boolean movedItems;

	public OrderDropperModule() {
		super("order_dropper", "Order Snipper", "Snipes profitable orders for the selected item and sells into them automatically.", "Money");
	}

	@Override
	protected void onEnable() {
		stage = Stage.OPEN_ORDERS;
		stageMillis = System.currentTimeMillis();
		stageTicks = 0;
		savedSyncId = -1;
		movedItems = false;
	}

	@Override
	public void onDisable() {
		stage = Stage.OPEN_ORDERS;
		stageMillis = 0L;
		stageTicks = 0;
		savedSyncId = -1;
		movedItems = false;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.world == null || client.interactionManager == null) {
			return;
		}

		Item targetItem = MoneyModuleUtil.resolveItem(targetItemId.getValue());
		if (targetItem == Items.AIR || minPrice.getValue() < 0.0) {
			return;
		}

		long now = System.currentTimeMillis();
		stageTicks++;
		if (shouldRestart(client, now)) {
			return;
		}

		switch (stage) {
			case OPEN_ORDERS -> {
				if (client.currentScreen instanceof GenericContainerScreen) {
					client.player.closeHandledScreen();
					next(Stage.CYCLE_PAUSE, now);
					return;
				}
				savedSyncId = -1;
				movedItems = false;
				MoneyModuleUtil.sendCommand(client, "/orders " + itemName.getValue());
				next(Stage.WAIT_ORDERS_GUI, now);
			}
			case WAIT_ORDERS_GUI -> {
				if (stageTicks < Math.max(1, delayTicks.getValue().intValue() / 2)) {
					return;
				}
				if (client.currentScreen instanceof GenericContainerScreen) {
					next(Stage.SELECT_ORDER, now);
				} else if (now - stageMillis > 2000) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case SELECT_ORDER -> {
				if (!(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();

				int slotsToScan = Math.min(4, MoneyModuleUtil.getContainerSlotCount(handler));
				for (int slotIndex = 0; slotIndex < slotsToScan; slotIndex++) {
					Slot slot = MoneyModuleUtil.getContainerSlot(handler, slotIndex);
					if (slot == null) {
						continue;
					}

					ItemStack stack = slot.getStack();
					if (!stack.isEmpty() && isMatchingOrder(client, stack, targetItem)) {
						savedSyncId = handler.syncId;
						movedItems = false;
						client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
						next(Stage.WAIT_DEPOSIT_GUI, now);
						return;
					}
				}

				if (MoneyModuleUtil.fastRefreshOrders(client, handler)) {
					next(Stage.WAIT_ORDERS_GUI, now);
					return;
				}

				if (now - stageMillis > 300L) {
					client.player.closeHandledScreen();
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case WAIT_DEPOSIT_GUI -> {
				if (client.currentScreen instanceof GenericContainerScreen screen && screen.getScreenHandler().syncId != savedSyncId) {
					next(Stage.TRANSFER_ITEMS, now);
					return;
				}
				if (now - stageMillis > 3000) {
					client.player.closeHandledScreen();
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case TRANSFER_ITEMS -> {
				if (!(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				boolean movedAny = false;
				for (Slot slot : handler.slots) {
					ItemStack stack = slot.getStack();
					if (!MoneyModuleUtil.isPlayerInventorySlot(client, handler, slot)) {
						continue;
					}
					if (stack.isOf(targetItem) || canUseShulker(client, stack, targetItem)) {
						client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, client.player);
						movedAny = true;
					}
				}

				if (movedAny) {
					movedItems = true;
					client.player.closeHandledScreen();
					next(Stage.WAIT_CONFIRM_GUI, now);
					return;
				}
				if (now - stageMillis > 1200L) {
					client.player.closeHandledScreen();
					next(Stage.CYCLE_PAUSE, now);
				}
			}
			case WAIT_CONFIRM_GUI -> {
				if (!movedItems) {
					next(Stage.CYCLE_PAUSE, now);
					return;
				}
				if (stageTicks < Math.max(2, delayTicks.getValue().intValue() / 2)) {
					return;
				}
				if (client.currentScreen instanceof GenericContainerScreen screen && hasConfirmButton(screen.getScreenHandler())) {
					next(Stage.CONFIRM_SALE, now);
				} else if (now - stageMillis > 2000L) {
					if (client.currentScreen instanceof GenericContainerScreen) {
						client.player.closeHandledScreen();
					}
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case CONFIRM_SALE -> {
				if (!(client.currentScreen instanceof GenericContainerScreen screen)) {
					return;
				}
				ScreenHandler handler = screen.getScreenHandler();
				for (Slot slot : handler.slots) {
					if (slot.getStack().isOf(Items.LIME_STAINED_GLASS_PANE) || slot.getStack().isOf(Items.GREEN_STAINED_GLASS_PANE)) {
						client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, client.player);
						next(Stage.FINAL_EXIT, now);
						return;
					}
				}
				if (now - stageMillis > 2000L) {
					client.player.closeHandledScreen();
					next(Stage.OPEN_ORDERS, now);
				}
			}
			case FINAL_EXIT -> {
				if (client.currentScreen instanceof GenericContainerScreen) {
					client.player.closeHandledScreen();
				}
				if (now - stageMillis >= Math.max(125L, delayTicks.getValue().intValue() * 75L)) {
					movedItems = false;
					savedSyncId = -1;
					next(Stage.CYCLE_PAUSE, now);
				}
			}
			case CYCLE_PAUSE -> {
				if (client.currentScreen instanceof GenericContainerScreen) {
					client.player.closeHandledScreen();
					return;
				}
				if (stageTicks >= Math.max(3, delayTicks.getValue().intValue())) {
					next(Stage.OPEN_ORDERS, now);
				}
			}
			default -> {
			}
		}
	}

	private boolean isMatchingOrder(MinecraftClient client, ItemStack stack, Item targetItem) {
		if (!stack.isOf(targetItem)) {
			return false;
		}
		double price = MoneyModuleUtil.tooltipPrice(client, stack);
		return price >= minPrice.getValue();
	}

	private boolean canUseShulker(MinecraftClient client, ItemStack stack, Item targetItem) {
		if (!shulkerSupport.getValue()) {
			return false;
		}
		String targetName = targetItem.getName().getString().toLowerCase(Locale.ROOT);
		return stack.getName().getString().toLowerCase(Locale.ROOT).contains("shulker")
			&& stack.getTooltip(Item.TooltipContext.create(client.world), client.player, net.minecraft.item.tooltip.TooltipType.BASIC)
				.stream()
				.anyMatch(text -> text.getString().toLowerCase(Locale.ROOT).contains(targetName));
	}

	private boolean hasConfirmButton(ScreenHandler handler) {
		for (Slot slot : handler.slots) {
			ItemStack stack = slot.getStack();
			if (stack.isOf(Items.LIME_STAINED_GLASS_PANE) || stack.isOf(Items.GREEN_STAINED_GLASS_PANE)) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldRestart(MinecraftClient client, long now) {
		if (stageMillis <= 0L) {
			return false;
		}
		if (client.currentScreen instanceof GenericContainerScreen && now - stageMillis >= 20_000) {
			forceRestart(client);
			return true;
		}
		if (stage != Stage.OPEN_ORDERS && stage != Stage.CYCLE_PAUSE && now - stageMillis >= 8_000) {
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
		stageTicks = 0;
	}
}
