package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.EnumSetting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;

public class EspModule extends Module {
	private final EnumSetting targetType = addSetting(new EnumSetting("target_type", "Target Type", "General", "Players", "Players", "All Living", "Hostile Mobs", "Passive Mobs", "Villagers", "Armor Stands", "Item Frames", "Thrown Pearls", "Storage Minecarts", "All Entities"));
	private final EnumSetting outlineColor = addSetting(new EnumSetting("outline_color", "Outline Color", "Color", "Red", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));

	public EspModule() {
		super("esp", "ESP", "Highlights selected entities with an outline", "Other");
	}

	public boolean shouldRender(Entity entity, PlayerEntity self) {
		if (entity == null || entity == self) return false;

		return switch (targetType.getValue()) {
			case "Players" -> entity instanceof PlayerEntity;
			case "All Living" -> entity instanceof LivingEntity;
			case "Hostile Mobs" -> entity instanceof HostileEntity;
			case "Passive Mobs" -> entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && !(entity instanceof HostileEntity) && !(entity instanceof VillagerEntity);
			case "Villagers" -> entity instanceof VillagerEntity;
			case "Armor Stands" -> entity instanceof ArmorStandEntity;
			case "Item Frames" -> entity instanceof ItemFrameEntity;
			case "Thrown Pearls" -> entity instanceof EnderPearlEntity;
			case "Storage Minecarts" -> entity instanceof ChestMinecartEntity || entity instanceof ChestBoatEntity;
			case "All Entities" -> true;
			default -> entity instanceof PlayerEntity;
		};
	}

	public String getOutlineColorName() {
		return outlineColor.getValue();
	}
}
