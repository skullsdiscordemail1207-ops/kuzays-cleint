package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;

public class StorageEspModule extends Module {
	private final BooleanSetting chest = addSetting(new BooleanSetting("chest", "Chest", "Storage Types", true));
	private final BooleanSetting trappedChest = addSetting(new BooleanSetting("trapped_chest", "Trapped Chest", "Storage Types", true));
	private final BooleanSetting barrel = addSetting(new BooleanSetting("barrel", "Barrel", "Storage Types", true));
	private final BooleanSetting shulker = addSetting(new BooleanSetting("shulker", "Shulker Box", "Storage Types", true));
	private final BooleanSetting enderChest = addSetting(new BooleanSetting("ender_chest", "Ender Chest", "Storage Types", true));
	private final BooleanSetting hopper = addSetting(new BooleanSetting("hopper", "Hopper", "Storage Types", false));
	private final BooleanSetting furnace = addSetting(new BooleanSetting("furnace", "Furnace", "Storage Types", false));
	private final BooleanSetting dispenserDropper = addSetting(new BooleanSetting("dispenser_dropper", "Dispenser / Dropper", "Storage Types", false));
	private final NumberSetting renderRadius = addSetting(new NumberSetting("render_radius", "Render Radius", "General", 24.0, 6.0, 64.0, 1.0));

	private final EnumSetting chestColor = addSetting(new EnumSetting("chest_color", "Chest Color", "Colors", "Yellow", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting trappedChestColor = addSetting(new EnumSetting("trapped_chest_color", "Trapped Chest Color", "Colors", "Red", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting barrelColor = addSetting(new EnumSetting("barrel_color", "Barrel Color", "Colors", "Orange", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting shulkerColor = addSetting(new EnumSetting("shulker_color", "Shulker Color", "Colors", "Purple", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting enderChestColor = addSetting(new EnumSetting("ender_chest_color", "Ender Chest Color", "Colors", "Cyan", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting hopperColor = addSetting(new EnumSetting("hopper_color", "Hopper Color", "Colors", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting furnaceColor = addSetting(new EnumSetting("furnace_color", "Furnace Color", "Colors", "Green", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));
	private final EnumSetting dispenserDropperColor = addSetting(new EnumSetting("dispenser_dropper_color", "Dispenser / Dropper Color", "Colors", "Blue", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));

	public StorageEspModule() {
		super("storage_esp", "Storage ESP", "Highlights selected storage blocks with per-block outline colors", "Other");
	}

	public boolean shouldRender(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.CHEST) return chest.getValue();
		if (block == Blocks.TRAPPED_CHEST) return trappedChest.getValue();
		if (block == Blocks.BARREL) return barrel.getValue();
		if (block instanceof ShulkerBoxBlock) return shulker.getValue();
		if (block == Blocks.ENDER_CHEST) return enderChest.getValue();
		if (block == Blocks.HOPPER) return hopper.getValue();
		if (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER) return furnace.getValue();
		if (block == Blocks.DISPENSER || block == Blocks.DROPPER) return dispenserDropper.getValue();
		return false;
	}

	public String getColorName(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.CHEST) return chestColor.getValue();
		if (block == Blocks.TRAPPED_CHEST) return trappedChestColor.getValue();
		if (block == Blocks.BARREL) return barrelColor.getValue();
		if (block instanceof ShulkerBoxBlock) return shulkerColor.getValue();
		if (block == Blocks.ENDER_CHEST) return enderChestColor.getValue();
		if (block == Blocks.HOPPER) return hopperColor.getValue();
		if (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER) return furnaceColor.getValue();
		if (block == Blocks.DISPENSER || block == Blocks.DROPPER) return dispenserDropperColor.getValue();
		return "White";
	}

	public int getRenderRadius() {
		return (int) Math.round(renderRadius.getValue());
	}
}
