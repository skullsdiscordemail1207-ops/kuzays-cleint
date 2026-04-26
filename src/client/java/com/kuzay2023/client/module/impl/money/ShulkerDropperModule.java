package com.kuzay2023.client.module.impl.money;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ShulkerDropperModule extends Module {
	private final NumberSetting delay = addSetting(new NumberSetting("delay_ticks", "Delay Ticks", "General", 1.0, 0.0, 20.0, 1.0));
	private int cooldown;

	public ShulkerDropperModule() {
		super("shulker_dropper", "Shulker Dropper", "Automatically buys shulkers from shop and drops them.", "Money");
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
			cooldown = 20;
			return;
		}

		if (handler.getSlot(11).getStack().isOf(Items.END_STONE) && handler.getSlot(11).getStack().getCount() == 1) {
			client.interactionManager.clickSlot(handler.syncId, 11, 0, SlotActionType.PICKUP, client.player);
			cooldown = 20;
			return;
		}

		if (handler.getSlot(17).getStack().isOf(Items.SHULKER_BOX)) {
			client.interactionManager.clickSlot(handler.syncId, 17, 0, SlotActionType.PICKUP, client.player);
			cooldown = 20;
			return;
		}

		if (handler.getSlot(13).getStack().isOf(Items.SHULKER_BOX)) {
			client.interactionManager.clickSlot(handler.syncId, 23, 0, SlotActionType.QUICK_MOVE, client.player);
			client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
			cooldown = delay.getValue().intValue();
		}
	}
}
