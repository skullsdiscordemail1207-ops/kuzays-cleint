package com.kuzay2023.client.hud;

import com.kuzay2023.client.gamble.RigService;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class RigHudElement extends HudElement {
	public RigHudElement() {
		super("rig_hud", "Pig HUD", 390.0F, 60.0F);
	}

	@Override
	public int getWidth() {
		return 24;
	}

	@Override
	public int getHeight() {
		return 24;
	}

	@Override
	public void render(DrawContext context, float globalScale) {
		if (!RigService.isRigEnabled() || !RigService.isHudVisible()) {
			return;
		}

		context.drawItem(new ItemStack(RigService.getWinnerItem()), 4, 4);
	}
}
