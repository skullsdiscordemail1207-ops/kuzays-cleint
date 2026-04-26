package com.kuzay2023.client.hud;

import java.util.List;
import java.util.Map;

import com.kuzay2023.client.gamble.HighestGamblersService;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class HighestGamblersHudElement extends HudElement {
	public HighestGamblersHudElement() {
		super("highest_gamblers", "Highest Payments", 8.0F, 8.0F);
	}

	@Override
	public int getWidth() {
		return HighestGamblersService.getHudWidth();
	}

	@Override
	public int getHeight() {
		return HighestGamblersService.getHudHeight();
	}

	@Override
	public void render(DrawContext context, float globalScale) {
		if (!HighestGamblersService.isEnabled()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		List<Map.Entry<String, java.math.BigDecimal>> leaders = HighestGamblersService.getTopThree();
		int width = HighestGamblersService.getHudWidth();
		int height = HighestGamblersService.getHudHeight();

		context.fill(0, 0, width, height, 0x88000000);
		drawBorder(context, 0, 0, width, height, 0x66FFFFFF);
		context.drawText(client.textRenderer, Text.literal("Highest payments"), 6, 6, 0xFFFFFF, false);

		int clearX = width - 42 - 6;
		int clearY = 5;
		context.fill(clearX, clearY, clearX + 42, clearY + 12, 0xAA772222);
		drawBorder(context, clearX, clearY, 42, 12, 0x99FFFFFF);
		context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("Clear"), clearX + 21, clearY + 2, 0xFFFFFF);

		int startY = 22;
		if (leaders.isEmpty()) {
			context.drawText(client.textRenderer, Text.literal("No payments tracked yet."), 6, startY, 0xAAAAAA, false);
			return;
		}

		for (int index = 0; index < 3; index++) {
			int lineY = startY + (index * 12);
			if (index < leaders.size()) {
				var entry = leaders.get(index);
				String line = (index + 1) + ". " + entry.getKey() + " - $" + HighestGamblersService.formatAmount(entry.getValue());
				context.drawText(client.textRenderer, Text.literal(line), 6, lineY, 0xFFFFFF, false);
			} else {
				context.drawText(client.textRenderer, Text.literal((index + 1) + ". ---"), 6, lineY, 0xAAAAAA, false);
			}
		}
	}

	private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
		context.drawHorizontalLine(x, x + width - 1, y, color);
		context.drawHorizontalLine(x, x + width - 1, y + height - 1, color);
		context.drawVerticalLine(x, y, y + height - 1, color);
		context.drawVerticalLine(x + width - 1, y, y + height - 1, color);
	}
}
