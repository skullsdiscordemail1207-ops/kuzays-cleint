package com.kuzay2023.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.gui.render.UiRenderer;
import com.kuzay2023.client.module.bridge.LinkedModule;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class LinkedModuleSettingsScreen extends Screen {
	private static final int GUI_WIDTH = 660;
	private static final int GUI_HEIGHT = 440;
	private static final int GROUP_ROW_HEIGHT = 28;
	private static final int SETTING_ROW_HEIGHT = 54;
	private static final int TEXT_ROW_HEIGHT = 62;

	private final KuzayClientContext context;
	private final Screen parent;
	private final LinkedModule module;
	private final meteordevelopment.meteorclient.systems.modules.Module runtimeModule;

	private final Map<Setting<?>, TextFieldWidget> textFields = new LinkedHashMap<>();

	private boolean bindingKey;
	private int contentScroll;

	public LinkedModuleSettingsScreen(KuzayClientContext context, Screen parent, LinkedModule module) {
		super(Text.literal(module.getName()));
		this.context = context;
		this.parent = parent;
		this.module = module;
		this.runtimeModule = module.getRuntimeModule();
	}

	@Override
	protected void init() {
		clearChildren();
		textFields.clear();

		for (Row row : buildRows()) {
			if (row.groupHeader() || !usesTextField(row.setting())) {
				continue;
			}

			TextFieldWidget field = new TextFieldWidget(textRenderer, left() + 356, top() + 120, textFieldWidth(row.setting()), 18, Text.literal(row.setting().title));
			field.setMaxLength(512);
			field.setText(displaySettingValue(row.setting()));
			addDrawableChild(field);
			textFields.put(row.setting(), field);
		}

		contentScroll = MathHelper.clamp(contentScroll, 0, maxContentScroll());
		layoutWidgets();
	}

	@Override
	protected void applyBlur() {
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		drawContext.fill(0, 0, width, height, 0xE60A1017);

		int textColor = context.configManager().getConfig().theme.textColor;
		int accentColor = context.configManager().getConfig().theme.accentColor;
		int transparencyAlpha = MathHelper.clamp((int) (context.configManager().getConfig().theme.uiTransparency * 255.0F), 88, 250);
		int panelColor = UiRenderer.withAlpha(context.configManager().getConfig().theme.panelColor, transparencyAlpha);
		int mutedColor = UiRenderer.withAlpha(context.configManager().getConfig().theme.backgroundColor, Math.min(255, transparencyAlpha + 10));

		UiRenderer.drawPanel(drawContext, left(), top(), GUI_WIDTH, GUI_HEIGHT, panelColor, 0xFF2E3632);
		UiRenderer.drawText(drawContext, textRenderer, module.getName(), left() + 20, top() + 18, textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, module.getDescription(), left() + 20, top() + 34, GUI_WIDTH - 40, UiRenderer.withAlpha(textColor, 155));

		UiRenderer.drawPanel(drawContext, left() + 20, top() + 60, 170, 34, mutedColor, 0xFF314037);
		UiRenderer.drawText(drawContext, textRenderer, "State", left() + 32, top() + 72, UiRenderer.withAlpha(textColor, 170));
		UiRenderer.drawPill(drawContext, left() + 104, top() + 67, 72, 20, module.isEnabled() ? 0xFF2EC27E : 0xFFAA2E57);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, module.isEnabled() ? "Enabled" : "Disabled", left() + 118, top() + 73, 44, 0xFFF7F8F9);

		UiRenderer.drawPanel(drawContext, left() + 202, top() + 60, 180, 34, mutedColor, 0xFF314037);
		UiRenderer.drawText(drawContext, textRenderer, "Keybind", left() + 214, top() + 72, UiRenderer.withAlpha(textColor, 170));
		UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingKey ? "Press a key..." : module.getBoundKeyName(), left() + 286, top() + 72, 82, textColor);

		UiRenderer.drawPanel(drawContext, left() + 394, top() + 60, 246, 34, mutedColor, 0xFF314037);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "These are the live client settings for this module.", left() + 406, top() + 65, 220, UiRenderer.withAlpha(textColor, 165));

		contentScroll = MathHelper.clamp(contentScroll, 0, maxContentScroll());
		layoutWidgets();
		drawContext.enableScissor(left() + 20, contentViewportTop(), left() + 640, contentViewportTop() + contentViewportHeight());

		int rowY = contentViewportTop() + 6 - contentScroll;
		for (Row row : buildRows()) {
			if (row.groupHeader()) {
				UiRenderer.drawPanel(drawContext, left() + 20, rowY, 620, GROUP_ROW_HEIGHT - 4, 0xFF1F2823, 0xFF324038);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, row.groupName(), left() + 32, rowY + 8, 260, textColor);
				rowY += GROUP_ROW_HEIGHT;
				continue;
			}

			Setting<?> setting = row.setting();
			int rowHeight = rowHeight(setting);
			UiRenderer.drawPanel(drawContext, left() + 20, rowY, 620, rowHeight - 4, mutedColor, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, row.groupName() + " / " + setting.title, left() + 32, rowY + 8, 280, textColor);

			String description = setting.description == null ? "" : setting.description;
			if (!description.isBlank()) {
				UiRenderer.drawWrappedText(drawContext, textRenderer, description, left() + 32, rowY + 22, 300, 2, UiRenderer.withAlpha(textColor, 155));
			}

			if (isTextDisplaySetting(setting)) {
				UiRenderer.drawWrappedText(drawContext, textRenderer, displaySettingValue(setting), left() + 356, rowY + 14, 252, 3, UiRenderer.withAlpha(textColor, 190));
				rowY += rowHeight;
				continue;
			}

			if (setting instanceof BoolSetting boolSetting) {
				UiRenderer.drawPill(drawContext, left() + 518, rowY + 15, 90, 20, boolSetting.get() ? 0xFF2EC27E : 0xFF5D6268);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, boolSetting.get() ? "ON" : "OFF", left() + 553, rowY + 21, 22, 0xFFF7F8F9);
			} else if (setting instanceof EnumSetting<?>) {
				UiRenderer.drawPill(drawContext, left() + 412, rowY + 15, 196, 20, 0xFF24312A);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, String.valueOf(setting.get()), left() + 424, rowY + 21, 172, textColor);
			} else if (setting instanceof ItemSetting) {
				UiRenderer.drawPill(drawContext, left() + 532, rowY + 15, 76, 20, accentColor);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, "Pick", left() + 558, rowY + 21, 26, 0xFF07131A);
			} else if (setting instanceof EntityTypeListSetting) {
				UiRenderer.drawPanel(drawContext, left() + 356, rowY + 15, 168, 20, 0xFF090B10, 0xFF868B90);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, displaySettingValue(setting), left() + 364, rowY + 21, 152, textColor);
				UiRenderer.drawPill(drawContext, left() + 532, rowY + 15, 76, 20, accentColor);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, "Pick", left() + 558, rowY + 21, 26, 0xFF07131A);
			} else if (isListSetting(setting)) {
				UiRenderer.drawTrimmedText(drawContext, textRenderer, "Comma-separated", left() + 470, rowY + 39, 138, UiRenderer.withAlpha(textColor, 135));
			} else if (isColorSetting(setting)) {
				UiRenderer.drawTrimmedText(drawContext, textRenderer, "R G B A", left() + 534, rowY + 39, 74, UiRenderer.withAlpha(textColor, 135));
			}

			rowY += rowHeight;
		}

		syncWidgetsFromSettings();
		super.render(drawContext, mouseX, mouseY, delta);
		drawContext.disableScissor();
		renderScrollBar(drawContext, left() + 644, contentViewportTop(), contentViewportHeight(), contentScroll, maxContentScroll(), accentColor);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (bindingKey) {
			module.setBoundKey(com.kuzay2023.client.module.Module.encodeMouseButton(button));
			context.configManager().save(context);
			bindingKey = false;
			return true;
		}
		commitTextFields();
		layoutWidgets();

		if (UiRenderer.isHovered(mouseX, mouseY, left() + 104, top() + 67, 72, 20)) {
			module.toggle();
			context.configManager().save(context);
			refreshWidgets();
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left() + 202, top() + 60, 180, 34)) {
			bindingKey = true;
			return true;
		}

		int rowY = contentViewportTop() + 6 - contentScroll;
		for (Row row : buildRows()) {
			if (row.groupHeader()) {
				rowY += GROUP_ROW_HEIGHT;
				continue;
			}

			Setting<?> setting = row.setting();
			int rowHeight = rowHeight(setting);
			if (setting instanceof BoolSetting boolSetting) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 518, rowY + 15, 90, 20)) {
					boolSetting.set(!boolSetting.get());
					afterSettingChange();
					return true;
				}
			} else if (setting instanceof EnumSetting<?>) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 412, rowY + 15, 196, 20)) {
					cycleEnumSetting(setting);
					afterSettingChange();
					return true;
				}
			} else if (setting instanceof ItemSetting) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 532, rowY + 15, 76, 20)) {
					openItemPicker(setting);
					return true;
				}
			} else if (setting instanceof EntityTypeListSetting) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 532, rowY + 15, 76, 20)) {
					openEntityPicker(setting);
					return true;
				}
			}

			rowY += rowHeight;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (UiRenderer.isHovered(mouseX, mouseY, left() + 20, contentViewportTop(), 620, contentViewportHeight())) {
			contentScroll = MathHelper.clamp(contentScroll - (int) (verticalAmount * 24.0), 0, maxContentScroll());
			layoutWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingKey) {
			module.setBoundKey(keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
			context.configManager().save(context);
			bindingKey = false;
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			if (commitFocusedField()) {
				return true;
			}
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && client != null) {
			commitTextFields();
			client.setScreen(parent);
			return true;
		}
		boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
		commitTextFields();
		return handled;
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		boolean handled = super.charTyped(chr, modifiers);
		commitTextFields();
		return handled;
	}

	@Override
	public void removed() {
		commitTextFields();
		super.removed();
	}

	private List<Row> buildRows() {
		List<Row> rows = new ArrayList<>();
		for (SettingGroup group : runtimeModule.settings) {
			List<Setting<?>> visibleSettings = new ArrayList<>();
			for (Setting<?> setting : group) {
				if (setting.isVisible()) {
					visibleSettings.add(setting);
				}
			}

			if (visibleSettings.isEmpty()) {
				continue;
			}

			rows.add(Row.group(group.name));
			for (Setting<?> setting : visibleSettings) {
				rows.add(Row.setting(group.name, setting));
			}
		}
		return rows;
	}

	private void layoutWidgets() {
		int rowY = contentViewportTop() + 6 - contentScroll;
		for (Row row : buildRows()) {
			if (row.groupHeader()) {
				rowY += GROUP_ROW_HEIGHT;
				continue;
			}

			Setting<?> setting = row.setting();
			TextFieldWidget field = textFields.get(setting);
			if (field != null) {
				positionField(field, left() + 356, rowY + 15, textFieldWidth(setting), 18, rowHeight(setting) - 4);
			}
			rowY += rowHeight(setting);
		}
	}

	private void positionField(TextFieldWidget field, int x, int y, int width, int height, int rowHeight) {
		boolean visible = isContentRowVisible(y - 14, rowHeight);
		field.setVisible(visible);
		field.setX(x);
		field.setY(y);
		if (!visible && field.isFocused()) {
			field.setFocused(false);
		}
	}

	private boolean isContentRowVisible(int rowY, int rowHeight) {
		int top = contentViewportTop();
		int bottom = top + contentViewportHeight();
		return rowY + rowHeight >= top && rowY <= bottom;
	}

	private void syncWidgetsFromSettings() {
		for (Map.Entry<Setting<?>, TextFieldWidget> entry : textFields.entrySet()) {
			if (!entry.getValue().isFocused()) {
				String currentValue = displaySettingValue(entry.getKey());
				if (!entry.getValue().getText().equals(currentValue)) {
					entry.getValue().setText(currentValue);
				}
			}
		}
	}

	private void commitTextFields() {
		boolean changed = false;
		for (Map.Entry<Setting<?>, TextFieldWidget> entry : textFields.entrySet()) {
			TextFieldWidget field = entry.getValue();
			if (!field.isFocused()) {
				changed |= applyTextField(entry.getKey(), field);
			}
		}
		if (changed) {
			refreshWidgets();
		}
	}

	private boolean commitFocusedField() {
		for (Map.Entry<Setting<?>, TextFieldWidget> entry : textFields.entrySet()) {
			TextFieldWidget field = entry.getValue();
			if (field.isFocused()) {
				boolean changed = applyTextField(entry.getKey(), field);
				field.setFocused(false);
				if (changed) {
					refreshWidgets();
				} else {
					field.setText(displaySettingValue(entry.getKey()));
				}
				return true;
			}
		}
		return false;
	}

	private boolean applyTextField(Setting<?> setting, TextFieldWidget field) {
		String currentValue = displaySettingValue(setting);
		String normalized = normalizeInput(setting, field.getText());
		if (normalized.equals(currentValue)) {
			field.setText(currentValue);
			return false;
		}

		if (setting.parse(normalized)) {
			Systems.save();
			context.configManager().save(context);
			field.setText(displaySettingValue(setting));
			return true;
		}

		field.setText(currentValue);
		return false;
	}

	private void cycleEnumSetting(Setting<?> setting) {
		Object value = setting.get();
		if (!(value instanceof Enum<?> enumValue)) {
			return;
		}

		Enum<?>[] constants = enumValue.getDeclaringClass().getEnumConstants();
		if (constants == null || constants.length == 0) {
			return;
		}

		int nextIndex = (enumValue.ordinal() + 1) % constants.length;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Setting<Enum<?>> enumSetting = (Setting) setting;
		enumSetting.set(constants[nextIndex]);
	}

	private void afterSettingChange() {
		Systems.save();
		context.configManager().save(context);
		refreshWidgets();
	}

	private void refreshWidgets() {
		int previousScroll = contentScroll;
		init();
		contentScroll = MathHelper.clamp(previousScroll, 0, maxContentScroll());
		layoutWidgets();
	}

	private void openItemPicker(Setting<?> setting) {
		if (client == null) {
			return;
		}

		client.setScreen(new ItemPickerScreen(this, itemId -> {
			if (setting.parse(itemId)) {
				Systems.save();
				context.configManager().save(context);
				refreshWidgets();
			}
		}));
	}

	private void openEntityPicker(Setting<?> setting) {
		if (client == null || !(setting instanceof EntityTypeListSetting entitySetting)) {
			return;
		}

		client.setScreen(new EntityPickerScreen(this, entitySetting, selected -> {
			if (entitySetting.set(selected)) {
				Systems.save();
				context.configManager().save(context);
				refreshWidgets();
			}
		}));
	}

	private boolean usesTextField(Setting<?> setting) {
		return !(setting instanceof BoolSetting)
			&& !(setting instanceof EnumSetting<?>)
			&& !(setting instanceof EntityTypeListSetting)
			&& !isTextDisplaySetting(setting);
	}

	private boolean isTextDisplaySetting(Setting<?> setting) {
		return setting.getClass().getSimpleName().equals("TextDisplaySetting");
	}

	private boolean isListSetting(Setting<?> setting) {
		return setting.getClass().getSimpleName().contains("ListSetting") || setting.get() instanceof List<?>;
	}

	private boolean isColorSetting(Setting<?> setting) {
		return setting.getClass().getSimpleName().equals("ColorSetting");
	}

	private int textFieldWidth(Setting<?> setting) {
		return setting instanceof ItemSetting || setting instanceof EntityTypeListSetting ? 168 : 252;
	}

	private int rowHeight(Setting<?> setting) {
		return isTextDisplaySetting(setting) ? TEXT_ROW_HEIGHT : SETTING_ROW_HEIGHT;
	}

	private String normalizeInput(Setting<?> setting, String rawText) {
		if (rawText == null) {
			return "";
		}
		if (setting.getClass().getSimpleName().equals("StringSetting")) {
			return rawText;
		}
		if (isListSetting(setting)) {
			String[] parts = rawText.split(",");
			List<String> cleaned = new ArrayList<>();
			for (String part : parts) {
				String trimmed = part.trim();
				if (!trimmed.isEmpty()) {
					cleaned.add(trimmed);
				}
			}
			return String.join(",", cleaned);
		}
		return rawText.trim();
	}

	private String displaySettingValue(Setting<?> setting) {
		Object value = setting.get();
		if (value == null) {
			return "";
		}

		if (value instanceof Item item) {
			return Registries.ITEM.getId(item).toString();
		}
		if (value instanceof java.util.Set<?> set) {
			return formatSetValue(set);
		}
		if (value instanceof Block block) {
			return Registries.BLOCK.getId(block).toString();
		}
		if (value instanceof meteordevelopment.meteorclient.systems.modules.Module meteorSettingModule) {
			return meteorSettingModule.name;
		}
		if (value instanceof Color color) {
			return color.r + " " + color.g + " " + color.b + " " + color.a;
		}
		if (value instanceof List<?> list) {
			return formatListValue(list);
		}
		return String.valueOf(value);
	}

	private String formatListValue(List<?> values) {
		List<String> parts = new ArrayList<>();
		for (Object value : values) {
			if (value instanceof Item item) {
				parts.add(Registries.ITEM.getId(item).toString());
			} else if (value instanceof Block block) {
				parts.add(Registries.BLOCK.getId(block).toString());
			} else if (value instanceof meteordevelopment.meteorclient.systems.modules.Module meteorSettingModule) {
				parts.add(meteorSettingModule.name);
			} else {
				parts.add(String.valueOf(value));
			}
		}
		return String.join(",", parts);
	}

	private String formatSetValue(java.util.Set<?> values) {
		List<String> parts = new ArrayList<>();
		for (Object value : values) {
			if (value instanceof EntityType<?> entityType) {
				parts.add(Registries.ENTITY_TYPE.getId(entityType).toString());
			} else if (value instanceof Item item) {
				parts.add(Registries.ITEM.getId(item).toString());
			} else if (value instanceof Block block) {
				parts.add(Registries.BLOCK.getId(block).toString());
			} else {
				parts.add(String.valueOf(value));
			}
		}
		parts.sort(String::compareToIgnoreCase);
		return String.join(", ", parts);
	}

	private int contentViewportTop() {
		return top() + 106;
	}

	private int contentViewportHeight() {
		return GUI_HEIGHT - 124;
	}

	private int contentHeight() {
		int height = 0;
		for (Row row : buildRows()) {
			height += row.groupHeader() ? GROUP_ROW_HEIGHT : rowHeight(row.setting());
		}
		return height;
	}

	private int maxContentScroll() {
		return Math.max(0, contentHeight() - (contentViewportHeight() - 12));
	}

	private void renderScrollBar(DrawContext drawContext, int x, int y, int height, int scroll, int maxScroll, int accentColor) {
		if (maxScroll <= 0) {
			return;
		}

		UiRenderer.fillRoundedRect(drawContext, x, y, 4, height, 2, 0x44212A28);
		int thumbHeight = Math.max(26, Math.round((height / (float) Math.max(1, contentHeight())) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((scroll / (float) maxScroll) * travel);
		UiRenderer.fillRoundedRect(drawContext, x, thumbY, 4, thumbHeight, 2, UiRenderer.withAlpha(accentColor, 220));
	}

	private int left() {
		return (width - GUI_WIDTH) / 2;
	}

	private int top() {
		return (height - GUI_HEIGHT) / 2;
	}

	private record Row(boolean groupHeader, String groupName, Setting<?> setting) {
		private static Row group(String groupName) {
			return new Row(true, groupName == null || groupName.isBlank() ? "General" : groupName, null);
		}

		private static Row setting(String groupName, Setting<?> setting) {
			String resolvedGroup = groupName == null || groupName.isBlank() ? "General" : groupName;
			return new Row(false, resolvedGroup, setting);
		}
	}
}
