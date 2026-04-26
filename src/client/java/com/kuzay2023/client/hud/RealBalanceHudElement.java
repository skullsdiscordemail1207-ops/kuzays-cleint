package com.kuzay2023.client.hud;

import com.kuzay2023.client.balance.BalanceHooks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class RealBalanceHudElement extends HudElement {
	public RealBalanceHudElement() {
		super("real_balance", "Real Balance", 18.0F, 156.0F);
	}

	@Override
	public int getWidth() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return 72;
		}
		return Math.max(48, client.textRenderer.getWidth(BalanceHooks.getFormattedRealBalance()));
	}

	@Override
	public int getHeight() {
		return 10;
	}

	@Override
	public void render(DrawContext context, float globalScale) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		context.drawText(client.textRenderer, BalanceHooks.getFormattedRealBalance(), 0, 0, 0x55FF55, true);
	}
}
