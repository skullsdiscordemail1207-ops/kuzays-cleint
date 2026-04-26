package com.kuzay2023.client.hud;

import java.util.ArrayList;
import java.util.List;

import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.gui.render.UiRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class FakeScoreboardHudElement extends HudElement {
	private String title = "Practice";
	private List<String> lines = List.of(
		"Rank: Knight",
		"Online: 142",
		"Coins: $12,500",
		"Streak: 3",
		"Mode: Crystal",
		"play.example.net"
	);

	public FakeScoreboardHudElement() {
		super("fake_scoreboard", "Fake Scoreboard", 18.0F, 72.0F);
	}

	@Override
	public int getWidth() {
		return 138;
	}

	@Override
	public int getHeight() {
		return 18 + (Math.max(1, lines.size()) * 10) + 12;
	}

	@Override
	public void render(DrawContext context, float globalScale) {
		MinecraftClient client = MinecraftClient.getInstance();
		int panelColor = UiRenderer.withAlpha(KuzayClientModClient.getContext().configManager().getConfig().theme.panelColor, 228);
		int accentColor = KuzayClientModClient.getContext().configManager().getConfig().theme.accentColor;
		int textColor = KuzayClientModClient.getContext().configManager().getConfig().theme.textColor;

		UiRenderer.drawPanel(context, 0, 0, getWidth(), getHeight(), panelColor, accentColor);
		UiRenderer.drawText(context, client.textRenderer, title, 10, 8, textColor);
		context.fill(10, 20, getWidth() - 10, 21, UiRenderer.withAlpha(accentColor, 210));

		int rowY = 26;
		for (String line : lines) {
			UiRenderer.drawTrimmedText(context, client.textRenderer, line, 10, rowY, getWidth() - 20, UiRenderer.withAlpha(textColor, 220));
			rowY += 10;
		}
	}

	public void update(String title, List<String> lines) {
		this.title = title == null || title.isBlank() ? "Practice" : title;
		List<String> cleaned = new ArrayList<>();
		for (String line : lines) {
			if (line != null && !line.isBlank()) {
				cleaned.add(line);
			}
		}
		this.lines = cleaned.isEmpty() ? List.of("No lines configured") : List.copyOf(cleaned);
	}
}
