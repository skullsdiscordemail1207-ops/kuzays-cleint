package com.kuzay2023.client.hud;

import com.kuzay2023.client.KuzayClientMod;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.gui.render.UiRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class WatermarkHudElement extends HudElement {
	public WatermarkHudElement() {
		super("watermark", "Watermark", 18.0F, 18.0F);
	}

	@Override
	public int getWidth() {
		return 132;
	}

	@Override
	public int getHeight() {
		return 32;
	}

	@Override
	public void render(DrawContext context, float globalScale) {
		MinecraftClient client = MinecraftClient.getInstance();
		int panelColor = KuzayClientModClient.getContext().configManager().getConfig().theme.panelColor;
		int accentColor = KuzayClientModClient.getContext().configManager().getConfig().theme.accentColor;
		int textColor = KuzayClientModClient.getContext().configManager().getConfig().theme.textColor;

		UiRenderer.drawPanel(context, 0, 0, getWidth(), getHeight(), panelColor, accentColor);
		UiRenderer.drawText(context, client.textRenderer, KuzayClientMod.DISPLAY_NAME, 10, 8, textColor);
		UiRenderer.drawText(context, client.textRenderer, "Framework 1.0.0", 10, 18, UiRenderer.withAlpha(textColor, 160));
	}
}
