package com.kuzay2023.client.module.impl.money;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ShopBuyerModule extends Module {
	private final NumberSetting delay = addSetting(new NumberSetting("delay_ticks", "Delay Ticks", "General", 15.0, 1.0, 100.0, 1.0));
	private final EnumSetting itemToBuy = addSetting(new EnumSetting("item", "Item", "General", "Obsidian", "Obsidian", "EndCrystal", "RespawnAnchor", "Glowstone", "TotemOfUndying", "EnderPearl", "GoldenApple", "ExperienceBottle", "SlowFallingArrow"));
	private final BooleanSetting autoDrop = addSetting(new BooleanSetting("auto_drop", "Auto Drop", "General", true));

	private int cooldown;

	public ShopBuyerModule() {
		super("shop_buyer", "Shop Buyer", "Automatically buys a selected item from the PVP shop category.", "Other");
	}

	@Override
	public void onDisable() {
		cooldown = 0;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.interactionManager == null) {
			return;
		}

		if (cooldown > 0) {
			cooldown--;
			return;
		}

		ScreenHandler handler = client.player.currentScreenHandler;
		if (!(handler instanceof net.minecraft.screen.GenericContainerScreenHandler container) || container.getRows() != 3) {
			MoneyModuleUtil.sendCommand(client, "/shop");
			cooldown = delay.getValue().intValue();
			return;
		}

		if (isBuyingScreen(handler)) {
			handleBuyingScreen(client, handler);
			return;
		}

		if (isPvpCategoryScreen(handler)) {
			Item target = selectedItem();
			int slot = MoneyModuleUtil.findSlot(handler, target);
			if (slot != -1) {
				client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
				cooldown = delay.getValue().intValue();
			}
			return;
		}

		if (handler.getSlot(13).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
			client.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, client.player);
			cooldown = delay.getValue().intValue();
		}
	}

	private boolean isPvpCategoryScreen(ScreenHandler handler) {
		return handler.getSlot(9).getStack().isOf(Items.OBSIDIAN)
			|| handler.getSlot(10).getStack().isOf(Items.END_CRYSTAL)
			|| handler.getSlot(11).getStack().isOf(Items.RESPAWN_ANCHOR)
			|| handler.getSlot(12).getStack().isOf(Items.GLOWSTONE);
	}

	private boolean isBuyingScreen(ScreenHandler handler) {
		return MoneyModuleUtil.findSlot(handler, Items.LIME_STAINED_GLASS_PANE) != -1;
	}

	private void handleBuyingScreen(MinecraftClient client, ScreenHandler handler) {
		int stack64 = MoneyModuleUtil.findSlot(handler, Items.LIME_STAINED_GLASS_PANE, 64);
		if (stack64 != -1) {
			client.interactionManager.clickSlot(handler.syncId, stack64, 0, SlotActionType.PICKUP, client.player);
			cooldown = delay.getValue().intValue();
			return;
		}

		int confirm = MoneyModuleUtil.findSlot(handler, Items.LIME_STAINED_GLASS_PANE);
		if (confirm != -1) {
			client.interactionManager.clickSlot(handler.syncId, confirm, 0, SlotActionType.PICKUP, client.player);
			if (autoDrop.getValue()) {
				client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
			}
			cooldown = delay.getValue().intValue();
		}
	}

	private Item selectedItem() {
		return switch (itemToBuy.getValue()) {
			case "EndCrystal" -> Items.END_CRYSTAL;
			case "RespawnAnchor" -> Items.RESPAWN_ANCHOR;
			case "Glowstone" -> Items.GLOWSTONE;
			case "TotemOfUndying" -> Items.TOTEM_OF_UNDYING;
			case "EnderPearl" -> Items.ENDER_PEARL;
			case "GoldenApple" -> Items.GOLDEN_APPLE;
			case "ExperienceBottle" -> Items.EXPERIENCE_BOTTLE;
			case "SlowFallingArrow" -> Items.TIPPED_ARROW;
			default -> Items.OBSIDIAN;
		};
	}
}
