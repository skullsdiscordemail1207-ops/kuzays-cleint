package com.kuzay2023.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.kuzay2023.client.gui.render.UiRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class OptionPickerScreen extends Screen {
	private final Screen parent;
	private final String titleText;
	private final List<String> options;
	private final Consumer<String> onPick;

	private TextFieldWidget searchField;
	private int scroll;

	public OptionPickerScreen(Screen parent, String titleText, List<String> options, Consumer<String> onPick) {
		super(Text.literal(titleText));
		this.parent = parent;
		this.titleText = titleText;
		this.options = List.copyOf(options);
		this.onPick = onPick;
	}

	@Override
	protected void init() {
		searchField = new TextFieldWidget(textRenderer, width / 2 - 140, 34, 280, 20, Text.literal("Search"));
		searchField.setMaxLength(128);
		addDrawableChild(searchField);
		setInitialFocus(searchField);
		setFocused(searchField);
		searchField.setFocused(true);
		scroll = 0;
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
	}

	@Override
	protected void applyBlur() {
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
	}

	@Override
	public void removed() {
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
		super.removed();
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		drawContext.fill(0, 0, width, height, 0xE60A0F16);

		int panelX = width / 2 - 190;
		int panelY = 18;
		int panelWidth = 380;
		int panelHeight = height - 36;
		int textColor = 0xFFF4F6F8;
		int accentColor = 0xFF4ADE80;
		int panelColor = 0xF21A211B;
		int mutedColor = 0xFF252D25;

		UiRenderer.drawPanel(drawContext, panelX, panelY, panelWidth, panelHeight, panelColor, 0xFF304030);
		UiRenderer.drawText(drawContext, textRenderer, titleText, panelX + 18, panelY + 14, textColor);
		UiRenderer.drawText(drawContext, textRenderer, "Pick a value from the glazed-style list.", panelX + 18, panelY + 28, UiRenderer.withAlpha(textColor, 150));

		List<String> filtered = filteredOptions();
		int listY = panelY + 66;
		int visibleRows = Math.max(1, (panelHeight - 86) / 30);
		int endIndex = Math.min(filtered.size(), scroll + visibleRows);

		for (int index = scroll; index < endIndex; index++) {
			String option = filtered.get(index);
			int rowY = listY + ((index - scroll) * 30);
			boolean hovered = UiRenderer.isHovered(mouseX, mouseY, panelX + 14, rowY, panelWidth - 28, 24);
			int optionColor = colorForOption(option);
			UiRenderer.drawPanel(drawContext, panelX + 14, rowY, panelWidth - 28, 24, hovered ? 0xFF334233 : mutedColor, hovered ? accentColor : 0xFF313A31);
			if (looksLikeColor(option)) {
				UiRenderer.fillRoundedRect(drawContext, panelX + 22, rowY + 6, 12, 12, 4, optionColor);
			}
			UiRenderer.drawTrimmedText(drawContext, textRenderer, option, panelX + (looksLikeColor(option) ? 42 : 24), rowY + 8, panelWidth - 72, looksLikeColor(option) ? optionColor : textColor);
		}

		if (filtered.isEmpty()) {
			UiRenderer.drawText(drawContext, textRenderer, "No options match that search.", panelX + 18, panelY + 74, UiRenderer.withAlpha(textColor, 160));
		}

		int maxScroll = Math.max(0, filtered.size() - visibleRows);
		int listHeight = visibleRows * 30;
		renderScrollBar(drawContext, panelX + panelWidth - 12, listY, listHeight, scroll, maxScroll, accentColor);

		super.render(drawContext, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		List<String> filtered = filteredOptions();
		int panelX = width / 2 - 190;
		int panelY = 18;
		int panelHeight = height - 36;
		int visibleRows = Math.max(1, (panelHeight - 86) / 30);
		int endIndex = Math.min(filtered.size(), scroll + visibleRows);
		int listY = panelY + 66;
		for (int index = scroll; index < endIndex; index++) {
			int rowY = listY + ((index - scroll) * 30);
			if (UiRenderer.isHovered(mouseX, mouseY, panelX + 14, rowY, panelWidth() - 28, 24)) {
				onPick.accept(filtered.get(index));
				if (client != null) {
					client.setScreen(parent);
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int maxScroll = Math.max(0, filteredOptions().size() - Math.max(1, ((height - 36) - 86) / 30));
		scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(verticalAmount)));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256 && client != null) {
			client.setScreen(parent);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private List<String> filteredOptions() {
		String query = searchField == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return options;
		}

		List<String> filtered = new ArrayList<>();
		for (String option : options) {
			if (option.toLowerCase(Locale.ROOT).contains(query)) {
				filtered.add(option);
			}
		}
		return filtered;
	}

	private int panelWidth() {
		return 380;
	}

	private boolean looksLikeColor(String option) {
		return switch (option) {
			case "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple" -> true;
			default -> false;
		};
	}

	private int colorForOption(String option) {
		return switch (option) {
			case "Red" -> 0xFFE34B4B;
			case "Green" -> 0xFF2EC27E;
			case "Blue" -> 0xFF409CFF;
			case "Yellow" -> 0xFFF0C54A;
			case "Cyan" -> 0xFF3BD6FF;
			case "Orange" -> 0xFFFF9933;
			case "Pink" -> 0xFFFF69B4;
			case "White" -> 0xFFFFFFFF;
			case "Purple" -> 0xFFA349FF;
			default -> 0xFFF4F6F8;
		};
	}

	private void renderScrollBar(DrawContext drawContext, int x, int y, int height, int scroll, int maxScroll, int accentColor) {
		if (maxScroll <= 0) {
			return;
		}

		UiRenderer.fillRoundedRect(drawContext, x, y, 4, height, 2, 0x44212A28);
		int thumbHeight = Math.max(16, Math.round((height / (float) (height + (maxScroll * 30))) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((scroll / (float) maxScroll) * travel);
		UiRenderer.fillRoundedRect(drawContext, x, thumbY, 4, thumbHeight, 2, UiRenderer.withAlpha(accentColor, 220));
	}
}
