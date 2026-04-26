package com.kuzay2023.client.gui.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class UiRenderer {
	private static final int DEFAULT_RADIUS = 6;

	private UiRenderer() {
	}

	public static void drawPanel(DrawContext context, int x, int y, int width, int height, int fillColor, int outlineColor) {
		drawRoundedPanel(context, x, y, width, height, DEFAULT_RADIUS, fillColor, outlineColor);
	}

	public static void drawPill(DrawContext context, int x, int y, int width, int height, int color) {
		drawRoundedPanel(context, x, y, width, height, Math.min(DEFAULT_RADIUS, Math.max(3, height / 2)), color, 0x22000000);
	}

	public static void drawRoundedPanel(DrawContext context, int x, int y, int width, int height, int radius, int fillColor, int outlineColor) {
		fillRoundedRect(context, x, y, width, height, radius, fillColor);
		drawRoundedOutline(context, x, y, width, height, radius, outlineColor | 0xFF000000);
	}

	public static void fillRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int effectiveRadius = Math.max(1, Math.min(radius, Math.min(width / 2, height / 2)));
		for (int row = 0; row < height; row++) {
			int inset = row < effectiveRadius
				? cornerInset(row, effectiveRadius)
				: row >= height - effectiveRadius
					? cornerInset(height - row - 1, effectiveRadius)
					: 0;
			context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
		}
	}

	public static void drawRoundedOutline(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		if (width <= 1 || height <= 1) {
			return;
		}

		int effectiveRadius = Math.max(1, Math.min(radius, Math.min(width / 2, height / 2)));
		for (int row = 0; row < height; row++) {
			int inset = row < effectiveRadius
				? cornerInset(row, effectiveRadius)
				: row >= height - effectiveRadius
					? cornerInset(height - row - 1, effectiveRadius)
					: 0;
			if (row == 0 || row == height - 1) {
				context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
			} else {
				context.fill(x + inset, y + row, x + inset + 1, y + row + 1, color);
				context.fill(x + width - inset - 1, y + row, x + width - inset, y + row + 1, color);
			}
		}
	}

	public static void drawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
		context.drawText(textRenderer, Text.literal(text), x, y, color, false);
	}

	public static void drawTrimmedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int maxWidth, int color) {
		drawText(context, textRenderer, fitText(textRenderer, text, maxWidth), x, y, color);
	}

	public static int drawWrappedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int maxWidth, int maxLines, int color) {
		List<String> lines = wrapText(textRenderer, text, maxWidth);
		int drawnLines = Math.min(maxLines, lines.size());
		for (int index = 0; index < drawnLines; index++) {
			String line = lines.get(index);
			if (index == maxLines - 1 && lines.size() > maxLines) {
				line = fitText(textRenderer, line + "...", maxWidth);
			}
			drawText(context, textRenderer, line, x, y + (index * 10), color);
		}
		return drawnLines;
	}

	public static String fitText(TextRenderer textRenderer, String text, int maxWidth) {
		if (maxWidth <= 0) {
			return "";
		}
		if (textRenderer.getWidth(text) <= maxWidth) {
			return text;
		}

		String ellipsis = "...";
		int ellipsisWidth = textRenderer.getWidth(ellipsis);
		if (ellipsisWidth >= maxWidth) {
			return textRenderer.trimToWidth(text, maxWidth);
		}
		return textRenderer.trimToWidth(text, maxWidth - ellipsisWidth).stripTrailing() + ellipsis;
	}

	private static List<String> wrapText(TextRenderer textRenderer, String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		if (maxWidth <= 0 || text.isBlank()) {
			lines.add("");
			return lines;
		}

		String[] paragraphs = text.split("\\n", -1);
		for (String paragraph : paragraphs) {
			if (paragraph.isBlank()) {
				lines.add("");
				continue;
			}

			String remaining = paragraph.strip();
			while (!remaining.isEmpty()) {
				String line = textRenderer.trimToWidth(remaining, maxWidth);
				if (line.isEmpty()) {
					break;
				}

				if (line.length() < remaining.length()) {
					int lastSpace = line.lastIndexOf(' ');
					if (lastSpace > 0) {
						line = line.substring(0, lastSpace);
					}
				}

				lines.add(line.stripTrailing());
				remaining = remaining.substring(line.length()).stripLeading();
			}
		}

		if (lines.isEmpty()) {
			lines.add("");
		}
		return lines;
	}

	public static int withAlpha(int rgb, int alpha) {
		return (MathHelper.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
	}

	public static boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	public static double lerp(double current, double target, double speed) {
		return current + (target - current) * MathHelper.clamp(speed, 0.0, 1.0);
	}

	private static int cornerInset(int row, int radius) {
		double normalized = (row + 0.5D) / radius;
		double curve = Math.sqrt(Math.max(0.0D, 1.0D - (normalized * normalized)));
		return Math.max(0, radius - (int) Math.round(curve * radius));
	}
}
