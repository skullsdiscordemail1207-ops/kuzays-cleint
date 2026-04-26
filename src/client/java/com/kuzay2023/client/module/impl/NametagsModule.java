package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class NametagsModule extends Module {
	private final BooleanSetting players = addSetting(new BooleanSetting("players", "Players", "Targets", true));
	private final BooleanSetting otherEntities = addSetting(new BooleanSetting("other_entities", "Other Entities", "Targets", false));
	private final BooleanSetting invisibleEntities = addSetting(new BooleanSetting("invisible_entities", "Invisible Entities", "Targets", true));
	private final BooleanSetting showHealth = addSetting(new BooleanSetting("show_health", "Show Health", "Display", true));
	private final BooleanSetting showHeldItem = addSetting(new BooleanSetting("show_held_item", "Show Held Item", "Display", true));
	private final BooleanSetting showArmor = addSetting(new BooleanSetting("show_armor", "Show Armor", "Display", true));
	private final BooleanSetting showEnchants = addSetting(new BooleanSetting("show_enchants", "Show Enchants", "Display", true));
	private final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Max Distance", "General", 128.0, 16.0, 256.0, 1.0));

	public NametagsModule() {
		super("nametags", "Nametags", "Shows custom nametags with health, held item, armor, and enchant abbreviations.", "Other");
	}

	public boolean shouldShow(Entity entity, PlayerEntity self) {
		if (entity == null || self == null || entity == self) {
			return false;
		}
		if (entity.squaredDistanceTo(self) > maxDistance.getValue() * maxDistance.getValue()) {
			return false;
		}
		if (entity.isInvisible() && !invisibleEntities.getValue()) {
			return false;
		}
		if (entity instanceof PlayerEntity) {
			return players.getValue();
		}
		return otherEntities.getValue() && entity instanceof LivingEntity;
	}

	public boolean shouldShowHealth() {
		return showHealth.getValue();
	}

	public boolean shouldShowHeldItem() {
		return showHeldItem.getValue();
	}

	public boolean shouldShowArmor() {
		return showArmor.getValue();
	}

	public boolean shouldShowEnchants() {
		return showEnchants.getValue();
	}
}
