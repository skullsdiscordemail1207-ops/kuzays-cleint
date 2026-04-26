package com.kuzay2023.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.kuzay2023.client.gui.render.UiRenderer;

import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BlockPickerScreen extends Screen {
	private final Screen parent;
	private final Consumer<String> onPick;

	private TextFieldWidget searchField;
	private List<BlockEntry> entries = List.of();
	private int scroll;

	public BlockPickerScreen(Screen parent, Consumer<String> onPick) {
		super(Text.literal("Block Picker"));
		this.parent = parent;
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
		entries = buildEntries();
		scroll = 0;
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		drawContext.fill(0, 0, width, height, 0xE60A0F16);

		int panelX = width / 2 - 180;
		int panelY = 18;
		int panelWidth = 360;
		int panelHeight = height - 36;
		int textColor = 0xFFF4F6F8;
		int accentColor = 0xFF4ADE80;
		int panelColor = 0xF21A211B;
		int mutedColor = 0xFF252D25;

		UiRenderer.drawPanel(drawContext, panelX, panelY, panelWidth, panelHeight, panelColor, 0xFF304030);
		UiRenderer.drawText(drawContext, textRenderer, "Pick Block", panelX + 18, panelY + 14, textColor);
		UiRenderer.drawText(drawContext, textRenderer, "Search by block name or id.", panelX + 18, panelY + 28, UiRenderer.withAlpha(textColor, 150));

		List<BlockEntry> filtered = filteredEntries();
		int listY = panelY + 66;
		int visibleRows = Math.max(1, (panelHeight - 86) / 30);
		int endIndex = Math.min(filtered.size(), scroll + visibleRows);
		for (int index = scroll; index < endIndex; index++) {
			BlockEntry entry = filtered.get(index);
			int rowY = listY + ((index - scroll) * 30);
			boolean hovered = UiRenderer.isHovered(mouseX, mouseY, panelX + 14, rowY, panelWidth - 28, 24);
			UiRenderer.drawPanel(drawContext, panelX + 14, rowY, panelWidth - 28, 24, hovered ? 0xFF334233 : mutedColor, hovered ? accentColor : 0xFF313A31);
			drawContext.drawItem(new ItemStack(entry.block()), panelX + 22, rowY + 4);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, entry.displayName(), panelX + 44, rowY + 5, panelWidth - 160, textColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, entry.id(), panelX + 44, rowY + 14, panelWidth - 160, UiRenderer.withAlpha(textColor, 145));
		}

		if (filtered.isEmpty()) {
			UiRenderer.drawText(drawContext, textRenderer, "No blocks match that search.", panelX + 18, panelY + 74, UiRenderer.withAlpha(textColor, 160));
		}

		super.render(drawContext, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		List<BlockEntry> filtered = filteredEntries();
		int panelX = width / 2 - 180;
		int panelY = 18;
		int panelHeight = height - 36;
		int visibleRows = Math.max(1, (panelHeight - 86) / 30);
		int endIndex = Math.min(filtered.size(), scroll + visibleRows);
		int listY = panelY + 66;
		for (int index = scroll; index < endIndex; index++) {
			int rowY = listY + ((index - scroll) * 30);
			if (UiRenderer.isHovered(mouseX, mouseY, panelX + 14, rowY, 332, 24)) {
				onPick.accept(filtered.get(index).id());
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
		int maxScroll = Math.max(0, filteredEntries().size() - Math.max(1, ((height - 36) - 86) / 30));
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

	private List<BlockEntry> buildEntries() {
		List<BlockEntry> built = new ArrayList<>();
		for (Identifier id : Registries.BLOCK.getIds()) {
			Block block = Registries.BLOCK.get(id);
			String displayName = block.getName().getString();
			built.add(new BlockEntry(id.toString(), displayName, block));
		}
		built.sort(Comparator.comparing(BlockEntry::displayName).thenComparing(BlockEntry::id));
		return built;
	}

	private List<BlockEntry> filteredEntries() {
		String query = searchField == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return entries;
		}
		return entries.stream()
			.filter(entry -> entry.displayName().toLowerCase(Locale.ROOT).contains(query) || entry.id().toLowerCase(Locale.ROOT).contains(query))
			.toList();
	}

	private record BlockEntry(String id, String displayName, Block block) {
	}
}
