package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class InvisEspModule extends Module {
	private final BooleanSetting showSelf = addSetting(new BooleanSetting("show_self", "Show Self", "General", false));

	public InvisEspModule() {
		super("invis_esp", "Invis ESP", "Outlines invisible players in red", "Other");
	}

	public boolean shouldRender(Entity entity, PlayerEntity self) {
		if (!(entity instanceof PlayerEntity player)) return false;
		if (player == self && !showSelf.getValue()) return false;
		return player.isInvisible();
	}
}
