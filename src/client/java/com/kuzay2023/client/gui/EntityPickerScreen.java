package com.kuzay2023.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import com.kuzay2023.client.gui.render.UiRenderer;

import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class EntityPickerScreen extends Screen {
	private final Screen parent;
	private final EntityTypeListSetting setting;
	private final Consumer<Set<EntityType<?>>> onSave;

	private TextFieldWidget searchField;
	private List<EntityEntry> entries = List.of();
	private Set<EntityType<?>> selected = Set.of();
	private int scroll;

	public EntityPickerScreen(Screen parent, EntityTypeListSetting setting, Consumer<Set<EntityType<?>>> onSave) {
		super(Text.literal("Entity Picker"));
		this.parent = parent;
		this.setting = setting;
		this.onSave = onSave;
	}

	@Override
	protected void init() {
		searchField = new TextFieldWidget(textRenderer, width / 2 - 160, 34, 320, 20, Text.literal("Search"));
		searchField.setMaxLength(128);
		addDrawableChild(searchField);
		setInitialFocus(searchField);
		setFocused(searchField);
		searchField.setFocused(true);
		entries = buildEntries();
		selected = new LinkedHashSet<>(setting.get());
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

		int panelX = width / 2 - 220;
		int panelY = 18;
		int panelWidth = 440;
		int panelHeight = height - 36;
		int textColor = 0xFFF4F6F8;
		int accentColor = 0xFF4ADE80;
		int panelColor = 0xF21A211B;
		int mutedColor = 0xFF252D25;

		UiRenderer.drawPanel(drawContext, panelX, panelY, panelWidth, panelHeight, panelColor, 0xFF304030);
		UiRenderer.drawText(drawContext, textRenderer, "Pick Entities", panelX + 18, panelY + 14, textColor);
		UiRenderer.drawText(drawContext, textRenderer, "Search every entity and click rows to toggle them.", panelX + 18, panelY + 28, UiRenderer.withAlpha(textColor, 150));
		UiRenderer.drawTrimmedText(drawContext, textRenderer, selectedSummary(), panelX + 18, panelY + 56, panelWidth - 36, UiRenderer.withAlpha(textColor, 170));

		UiRenderer.drawPill(drawContext, panelX + panelWidth - 182, panelY + 52, 76, 20, 0xFF2B7FFF);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Save", panelX + panelWidth - 156, panelY + 58, 28, 0xFFF6F9FC);
		UiRenderer.drawPill(drawContext, panelX + panelWidth - 94, panelY + 52, 76, 20, 0xFF434A46);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Cancel", panelX + panelWidth - 67, panelY + 58, 34, 0xFFF6F9FC);

		List<EntityEntry> filtered = filteredEntries();
		int listY = panelY + 84;
		int visibleRows = Math.max(1, (panelHeight - 102) / 26);
		int endIndex = Math.min(filtered.size(), scroll + visibleRows);
		for (int index = scroll; index < endIndex; index++) {
			EntityEntry entry = filtered.get(index);
			int rowY = listY + ((index - scroll) * 26);
			boolean hovered = UiRenderer.isHovered(mouseX, mouseY, panelX + 14, rowY, panelWidth - 28, 22);
			boolean picked = selected.contains(entry.entityType());
			UiRenderer.drawPanel(drawContext, panelX + 14, rowY, panelWidth - 28, 22, picked ? 0xFF28412D : (hovered ? 0xFF334233 : mutedColor), picked ? accentColor : 0xFF313A31);
			drawContext.fill(panelX + 22, rowY + 6, panelX + 30, rowY + 14, picked ? accentColor : 0xFF5B635D);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, entry.displayName(), panelX + 40, rowY + 5, panelWidth - 190, textColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, entry.id(), panelX + 40, rowY + 13, panelWidth - 190, UiRenderer.withAlpha(textColor, 145));
		}

		if (filtered.isEmpty()) {
			UiRenderer.drawText(drawContext, textRenderer, "No entities match that search.", panelX + 18, panelY + 92, UiRenderer.withAlpha(textColor, 160));
		}

		super.render(drawContext, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int panelX = width / 2 - 220;
		int panelY = 18;
		int panelWidth = 440;
		int panelHeight = height - 36;

		if (UiRenderer.isHovered(mouseX, mouseY, panelX + panelWidth - 182, panelY + 52, 76, 20)) {
			onSave.accept(new LinkedHashSet<>(selected));
			if (client != null) {
				client.setScreen(parent);
			}
			return true;
		}

		if (UiRenderer.isHovered(mouseX, mouseY, panelX + panelWidth - 94, panelY + 52, 76, 20)) {
			if (client != null) {
				client.setScreen(parent);
			}
			return true;
		}

		List<EntityEntry> filtered = filteredEntries();
		int visibleRows = Math.max(1, (panelHeight - 102) / 26);
		int endIndex = Math.min(filtered.size(), scroll + visibleRows);
		int listY = panelY + 84;
		for (int index = scroll; index < endIndex; index++) {
			int rowY = listY + ((index - scroll) * 26);
			if (UiRenderer.isHovered(mouseX, mouseY, panelX + 14, rowY, panelWidth - 28, 22)) {
				toggle(filtered.get(index).entityType());
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int maxScroll = Math.max(0, filteredEntries().size() - Math.max(1, ((height - 36) - 102) / 26));
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

	private List<EntityEntry> buildEntries() {
		List<EntityEntry> built = new ArrayList<>();
		for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
			EntityType<?> entityType = Registries.ENTITY_TYPE.get(id);
			if (entityType == null || !setting.filter.test(entityType)) {
				continue;
			}

			String displayName = entityType.getName().getString();
			built.add(new EntityEntry(id.toString(), displayName, entityType));
		}
		built.sort(Comparator.comparing(EntityEntry::displayName).thenComparing(EntityEntry::id));
		return built;
	}

	private List<EntityEntry> filteredEntries() {
		String query = searchField == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return entries;
		}
		return entries.stream()
			.filter(entry -> entry.displayName().toLowerCase(Locale.ROOT).contains(query) || entry.id().toLowerCase(Locale.ROOT).contains(query))
			.toList();
	}

	private void toggle(EntityType<?> entityType) {
		Set<EntityType<?>> updated = new LinkedHashSet<>(selected);
		if (!updated.remove(entityType)) {
			updated.add(entityType);
		}
		selected = updated;
	}

	private String selectedSummary() {
		if (selected.isEmpty()) {
			return "Selected: none";
		}

		List<String> names = new ArrayList<>();
		for (EntityType<?> entityType : selected) {
			names.add(Registries.ENTITY_TYPE.getId(entityType).toString());
		}
		names.sort(String::compareToIgnoreCase);
		return "Selected: " + String.join(", ", names);
	}

	private record EntityEntry(String id, String displayName, EntityType<?> entityType) {
	}
}
