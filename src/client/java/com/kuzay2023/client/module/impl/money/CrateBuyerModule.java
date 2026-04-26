package com.kuzay2023.client.module.impl.money;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class CrateBuyerModule extends Module {
	private final EnumSetting itemType = addSetting(new EnumSetting("item_type", "Item Type", "General", "All", "All", "Helmet", "Chestplate", "Leggings", "Boots", "Sword", "Pickaxe", "Shovel"));
	private final NumberSetting delay = addSetting(new NumberSetting("delay_ticks", "Delay Ticks", "General", 5.0, 5.0, 20.0, 1.0));
	private final BooleanSetting notifications = addSetting(new BooleanSetting("notifications", "Notifications", "General", true));

	private int tickCounter;
	private int currentStep;
	private int currentItemIndex;
	private boolean clickedOnce;

	public CrateBuyerModule() {
		super("crate_buyer", "Crate Buyer", "Automatically buys items from the common crate.", "Other");
	}

	@Override
	public void onDisable() {
		tickCounter = 0;
		currentStep = 0;
		currentItemIndex = 0;
		clickedOnce = false;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.interactionManager == null || !(client.currentScreen instanceof HandledScreen<?> screen)) {
			return;
		}

		if (!clickedOnce && !isValidCrateScreen(screen)) {
			return;
		}

		tickCounter++;
		if (tickCounter < delay.getValue().intValue()) {
			return;
		}
		tickCounter = 0;

		if ("All".equals(itemType.getValue())) {
			String[] sequence = {"Helmet", "Chestplate", "Leggings", "Boots", "Sword", "Pickaxe", "Shovel"};
			handleClick(client, screen, sequence[currentItemIndex]);
			if (currentStep == 0) {
				currentItemIndex = (currentItemIndex + 1) % sequence.length;
			}
		} else {
			handleClick(client, screen, itemType.getValue());
		}
	}

	private void handleClick(MinecraftClient client, HandledScreen<?> screen, String item) {
		int syncId = screen.getScreenHandler().syncId;
		if (currentStep == 0) {
			client.interactionManager.clickSlot(syncId, itemSlot(item), 0, SlotActionType.PICKUP, client.player);
			clickedOnce = true;
			currentStep = 1;
			return;
		}

		client.interactionManager.clickSlot(syncId, 15, 0, SlotActionType.PICKUP, client.player);
		currentStep = 0;
	}

	private boolean isValidCrateScreen(HandledScreen<?> screen) {
		for (int index = 0; index <= 9; index++) {
			if (!screen.getScreenHandler().getSlot(index).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE)) {
				return false;
			}
		}

		for (int index = 17; index <= 26; index++) {
			if (!screen.getScreenHandler().getSlot(index).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE)) {
				return false;
			}
		}

		return true;
	}

	private int itemSlot(String item) {
		return switch (item) {
			case "Chestplate" -> 11;
			case "Leggings" -> 12;
			case "Boots" -> 13;
			case "Sword" -> 14;
			case "Pickaxe" -> 15;
			case "Shovel" -> 16;
			default -> 10;
		};
	}
}
