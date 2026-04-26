package com.kuzay2023.client.module.impl.money;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AhSellModule extends Module {
	private final StringSetting sellPrice = addSetting(new StringSetting("sell_price", "Sell Price", "General", "30k"));
	private final NumberSetting confirmDelay = addSetting(new NumberSetting("confirm_delay", "Confirm Delay", "General", 10.0, 0.0, 100.0, 1.0));
	private final BooleanSetting notifications = addSetting(new BooleanSetting("notifications", "Notifications", "General", true));
	private final BooleanSetting enableFilter = addSetting(new BooleanSetting("enable_item_filter", "Enable Item Filter", "General", false));
	private final StringSetting filterItemId = addSetting(new StringSetting("filter_item_id", "Filter Item", "General", "minecraft:diamond"));

	private int delayCounter;
	private boolean awaitingConfirmation;
	private int currentSlot;

	public AhSellModule() {
		super("ah_sell", "Ah Sell", "Automatically sells all hotbar items using /ah sell.", "Money");
	}

	@Override
	protected void onEnable() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.getNetworkHandler() == null) {
			setEnabled(false);
			return;
		}

		if (!isValidPrice(sellPrice.getValue())) {
			setEnabled(false);
			return;
		}

		if (!hasSellableItemsInHotbar(client)) {
			setEnabled(false);
			return;
		}

		delayCounter = 0;
		awaitingConfirmation = false;
		currentSlot = 0;
		attemptSellCurrentSlot(client);
	}

	@Override
	protected void onDisable() {
		delayCounter = 0;
		awaitingConfirmation = false;
		currentSlot = 0;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (!awaitingConfirmation || client.player == null || client.interactionManager == null) {
			return;
		}

		if (delayCounter > 0) {
			delayCounter--;
			return;
		}

		ScreenHandler screenHandler = client.player.currentScreenHandler;
		if (screenHandler instanceof GenericContainerScreenHandler handler && handler.getRows() == 3) {
			ItemStack confirmButton = handler.getSlot(15).getStack();
			if (!confirmButton.isEmpty()) {
				client.interactionManager.clickSlot(handler.syncId, 15, 1, SlotActionType.QUICK_MOVE, client.player);
			}
			awaitingConfirmation = false;
			moveToNextSlot(client);
		}
	}

	private void attemptSellCurrentSlot(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null) {
			setEnabled(false);
			return;
		}

		if (currentSlot > 8) {
			setEnabled(false);
			return;
		}

		client.player.getInventory().selectedSlot = currentSlot;
		ItemStack stack = client.player.getInventory().getStack(currentSlot);
		if (stack.isEmpty()) {
			moveToNextSlot(client);
			return;
		}

		if (enableFilter.getValue()) {
			Item filterItem = MoneyModuleUtil.resolveItem(filterItemId.getValue());
			if (filterItem == net.minecraft.item.Items.AIR || !stack.isOf(filterItem)) {
				moveToNextSlot(client);
				return;
			}
		}

		double parsedPrice = MoneyModuleUtil.parsePrice(sellPrice.getValue().trim());
		if (parsedPrice <= 0.0D) {
			setEnabled(false);
			return;
		}

		client.getNetworkHandler().sendChatCommand("ah sell " + sellPrice.getValue().trim());
		delayCounter = confirmDelay.getValue().intValue();
		awaitingConfirmation = true;
	}

	private void moveToNextSlot(MinecraftClient client) {
		currentSlot++;
		attemptSellCurrentSlot(client);
	}

	private boolean hasSellableItemsInHotbar(MinecraftClient client) {
		Item filterItem = enableFilter.getValue() ? MoneyModuleUtil.resolveItem(filterItemId.getValue()) : net.minecraft.item.Items.AIR;
		for (int slot = 0; slot <= 8; slot++) {
			ItemStack stack = client.player.getInventory().getStack(slot);
			if (stack.isEmpty()) {
				continue;
			}

			if (!enableFilter.getValue() || (filterItem != net.minecraft.item.Items.AIR && stack.isOf(filterItem))) {
				return true;
			}
		}
		return false;
	}

	private boolean isValidPrice(String price) {
		return MoneyModuleUtil.parsePrice(price) > 0.0D;
	}

	public boolean notificationsEnabled() {
		return notifications.getValue();
	}
}
