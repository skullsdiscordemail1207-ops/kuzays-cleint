package com.kuzay2023.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.config.ThemeConfig;
import com.kuzay2023.client.config.ThemePreset;
import com.kuzay2023.client.config.ThemePresets;
import com.kuzay2023.client.gui.render.UiRenderer;
import com.kuzay2023.client.hud.HudElement;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.profile.ProfileEntry;
import com.kuzay2023.client.tab.ClientTab;
import com.kuzay2023.client.tab.ServerTabManager;
import com.kuzay2023.client.tab.VipAccessManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class ClientScreen extends Screen {
	private static final int BASE_WIDTH = 940;
	private static final int BASE_HEIGHT = 530;
	private static final Identifier TROPICAL_FISH_TEXTURE = Identifier.of("kuzay2023sclient", "textures/gui/tropical_fish_sheet.png");
	private static final Identifier TROPICAL_WATER_TEXTURE = Identifier.of("kuzay2023sclient", "textures/gui/tropical_water_overlay.png");

	private final KuzayClientContext context;

	private ScreenPanel activePanel = ScreenPanel.MODULES;
	private Module selectedModule;
	private String selectedCategory = ServerTabManager.globalTabId("Gambles");
	private String selectedColorTarget = "accent";
	private int moduleScroll;
	private int keybindScroll;
	private int hudScroll;
	private int profileScroll;
	private boolean bindingGuiKey;
	private boolean bindingPanicKey;
	private KeyBinding bindingVanillaKey;
	private double openProgress;
	private String selectedProfileName = "";
	private String selectedServerKey = "";
	private int settingsScroll;
	private int themePresetScroll;
	private boolean showHiddenModulesSettings;

	private TextFieldWidget searchField;
	private TextFieldWidget profileNameField;
	private ScrollDragTarget scrollDragTarget;
	private Module tabPickerModule;
	private int tabPickerX;
	private int tabPickerY;

	// Stars background
	private static final int STAR_COUNT = 120;
	private static final float[] STAR_X = new float[STAR_COUNT];
	private static final float[] STAR_Y = new float[STAR_COUNT];
	private static final float[] STAR_SPEED = new float[STAR_COUNT];
	private static final float[] STAR_SIZE = new float[STAR_COUNT];
	private static final int[] STAR_ALPHA = new int[STAR_COUNT];
	private static boolean starsInitialized = false;

	private static void initStars() {
		if (starsInitialized) return;
		Random rng = new Random(0xDEADBEEFL);
		for (int i = 0; i < STAR_COUNT; i++) {
			STAR_X[i] = rng.nextFloat();
			STAR_Y[i] = rng.nextFloat();
			STAR_SPEED[i] = 0.00004f + rng.nextFloat() * 0.00012f;
			STAR_SIZE[i] = 1 + rng.nextInt(3);
			STAR_ALPHA[i] = 150 + rng.nextInt(90);
		}
		starsInitialized = true;
	}

	public ClientScreen(KuzayClientContext context, Text title) {
		super(title);
		this.context = context;
	}

	@Override
	protected void init() {
		initStars();
		if (selectedModule == null || selectedModule.isHidden()) {
			List<Module> visibleModules = visibleModules();
			selectedModule = visibleModules.isEmpty() ? null : visibleModules.get(0);
		}
		searchField = new TextFieldWidget(textRenderer, sx(708), sy(50), ss(188), ss(20), Text.literal("Search"));
		searchField.setMaxLength(64);
		addDrawableChild(searchField);
		setInitialFocus(searchField);
		searchField.setFocused(activePanel == ScreenPanel.MODULES || activePanel == ScreenPanel.KEYBINDS);
		profileNameField = new TextFieldWidget(textRenderer, sx(36), sy(126), ss(232), ss(20), Text.literal("Profile Name"));
		profileNameField.setMaxLength(48);
		addDrawableChild(profileNameField);
		if (selectedServerKey.isBlank()) {
			selectedServerKey = context.configManager().getConfig().activeServerKey == null ? "" : context.configManager().getConfig().activeServerKey;
		}
		if (!context.profileManager().getProfiles().isEmpty() && selectedProfileName.isBlank()) {
			selectedProfileName = context.profileManager().getProfiles().get(0).name;
			profileNameField.setText(selectedProfileName);
		}
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
		openProgress = UiRenderer.lerp(openProgress, 1.0, 0.18);
		ThemeConfig theme = context.configManager().getConfig().theme;
		int transparencyAlpha = MathHelper.clamp((int) (theme.uiTransparency * 255.0F), 88, 250);
		drawContext.fill(0, 0, width, height, UiRenderer.withAlpha(theme.backgroundColor, Math.min(255, transparencyAlpha + 22)));

		int panelColor = UiRenderer.withAlpha(theme.panelColor, transparencyAlpha);
		int mutedPanel = UiRenderer.withAlpha(theme.backgroundColor, Math.min(255, transparencyAlpha + 8));
		int cardPanel = UiRenderer.withAlpha(theme.panelColor, Math.max(72, transparencyAlpha - 18));
		int accent = theme.accentColor;
		int text = theme.textColor;

		searchField.setVisible(activePanel == ScreenPanel.MODULES || activePanel == ScreenPanel.KEYBINDS);
		profileNameField.setVisible(activePanel == ScreenPanel.PROFILES);

		UiRenderer.drawPanel(drawContext, sx(0), sy(0), ss(BASE_WIDTH), ss(BASE_HEIGHT), panelColor, 0xFF252D28);
		if (theme.tropicalOverlayEnabled) {
			renderTropicalWaterOverlay(drawContext, theme, delta);
		}

		// Header bar — title lives here, above the tab pills
		UiRenderer.drawPanel(drawContext, sx(12), sy(8), ss(BASE_WIDTH - 24), ss(6), mutedPanel, 0xFF242D27);

		// Tab navigation bar — separate row below the title
		UiRenderer.drawPanel(drawContext, sx(12), sy(20), ss(BASE_WIDTH - 24), ss(28), mutedPanel, 0xFF242D27);
		renderTopNavigation(drawContext, text, accent);

		renderRightPreview(drawContext, text, accent, mutedPanel);

		switch (activePanel) {
			case MODULES -> renderModules(drawContext, 20, 54, 670, 456, text, accent, mutedPanel, cardPanel, mouseX, mouseY);
			case KEYBINDS -> renderKeybinds(drawContext, 20, 54, 670, 456, text, accent, mutedPanel);
			case SETTINGS -> renderSettings(drawContext, 20, 54, 670, 456, text, accent, mutedPanel);
			case HUD -> renderHudPanel(drawContext, 20, 54, 670, 456, text, accent, mutedPanel);
			case PROFILES -> renderProfiles(drawContext, 20, 54, 670, 456, text, accent, mutedPanel);
			case SERVERS -> renderServers(drawContext, 20, 54, 670, 456, text, accent, mutedPanel);
			default -> {
			}
		}

		if (theme.starsEnabled) {
			renderStars(drawContext, theme, delta);
		}

		super.render(drawContext, mouseX, mouseY, delta);
	}

	private void renderStars(DrawContext drawContext, ThemeConfig theme, float delta) {
		drawContext.enableScissor(sx(0), sy(0), sx(BASE_WIDTH), sy(BASE_HEIGHT));
		for (int i = 0; i < STAR_COUNT; i++) {
			STAR_X[i] -= STAR_SPEED[i];
			if (STAR_X[i] < 0f) {
				STAR_X[i] += 1f;
			}

			int localX = MathHelper.clamp((int) (STAR_X[i] * BASE_WIDTH), 0, BASE_WIDTH - 1);
			int localY = MathHelper.clamp((int) (STAR_Y[i] * BASE_HEIGHT), 0, BASE_HEIGHT - 1);
			int size = Math.max(1, Math.round(STAR_SIZE[i]));
			if (!isStarVisible(localX, localY, size)) {
				continue;
			}

			int starX = sx(localX);
			int starY = sy(localY);
			int starSize = ss(size);
			int glowPad = ss(1);
			int glowColor = UiRenderer.withAlpha(theme.accentColor, Math.max(110, STAR_ALPHA[i] - 30));
			int color = UiRenderer.withAlpha(theme.textColor, Math.min(255, STAR_ALPHA[i] + 35));
			drawContext.fill(starX - glowPad, starY - glowPad, starX + starSize + glowPad, starY + starSize + glowPad, glowColor);
			drawContext.fill(starX, starY, starX + starSize, starY + starSize, color);
		}
		drawContext.disableScissor();
	}

	private void renderTropicalWaterOverlay(DrawContext drawContext, ThemeConfig theme, float delta) {
		drawContext.enableScissor(sx(0), sy(0), sx(BASE_WIDTH), sy(BASE_HEIGHT));

		long now = System.currentTimeMillis();
		fillLocalRect(drawContext, 0, 0, BASE_WIDTH, BASE_HEIGHT, UiRenderer.withAlpha(0xFF0D4454, 18));

		if (hasGuiTexture(TROPICAL_FISH_TEXTURE)) {
			renderFishBand(drawContext, now, 76, 182, 96, 0.30F, true, 0.15D);
			renderFishBand(drawContext, now, 164, 170, 90, 0.24F, false, 1.25D);
			renderFishBand(drawContext, now, 264, 188, 100, 0.22F, true, 2.30D);
			renderFishBand(drawContext, now, 366, 164, 88, 0.18F, false, 3.35D);
		}

		if (hasGuiTexture(TROPICAL_WATER_TEXTURE)) {
			renderWaterTextureOverlay(drawContext, now);
		} else {
			renderFallbackWaterOverlay(drawContext, now);
		}

		drawContext.disableScissor();
	}

	private void renderTopNavigation(DrawContext drawContext, int textColor, int accentColor) {
		int x = 20;
		for (ScreenPanel panel : ScreenPanel.values()) {
			String label = switch (panel) {
				case MODULES -> "MODS";
				case KEYBINDS -> "KEYBINDS";
				case SETTINGS -> "SETTINGS";
				case HUD -> "HUD";
				case PROFILES -> "PROFILES";
				case SERVERS -> "SERVERS";
			};
			boolean active = panel == activePanel;
			int width = Math.max(90, textRenderer.getWidth(label) + 34);
			UiRenderer.drawPill(drawContext, sx(x), sy(22), ss(width), ss(20), active ? accentColor : 0xFF212924);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, label, sx(x + 14), sy(28), ss(width - 28), active ? 0xFF081217 : textColor);
			x += width + 10;
		}
	}

	private void renderModules(DrawContext drawContext, int x, int y, int width, int height, int textColor, int accentColor, int mutedPanel, int cardPanel, int mouseX, int mouseY) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(height), mutedPanel, 0xFF283029);

		int categoryX = x + 16;
		for (ClientTab category : categories()) {
			boolean active = selectedCategory.equals(category.id());
			int pillWidth = Math.max(54, textRenderer.getWidth(category.label()) + 24);
			UiRenderer.drawPill(drawContext, sx(categoryX), sy(y + 12), ss(pillWidth), ss(18), active ? accentColor : 0xFF263029);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, category.label().toUpperCase(Locale.ROOT), sx(categoryX + 12), sy(y + 18), ss(pillWidth - 24), active ? 0xFF081217 : textColor);
			categoryX += pillWidth + 8;
		}

		List<Module> modules = filteredModules();
		int contentX = x + 16;
		int contentY = y + 46 - moduleScroll;
		int cardWidth = 202;
		int cardHeight = 126;
		int gap = 12;
		int viewportHeight = 386;
		int contentHeight = Math.max(viewportHeight, ((int) Math.ceil(modules.size() / 3.0) * (cardHeight + gap)));
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		moduleScroll = MathHelper.clamp(moduleScroll, 0, maxScroll);
		int clipLeft = sx(x + 14);
		int clipTop = sy(y + 44);
		int clipRight = sx(x + width - 14);
		int clipBottom = sy(y + height - 14);
		drawContext.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			int column = index % 3;
			int row = index / 3;
			int cardX = contentX + (column * (cardWidth + gap));
			int cardY = contentY + (row * (cardHeight + gap));
			boolean hovered = UiRenderer.isHovered(mouseX, mouseY, sx(cardX), sy(cardY), ss(cardWidth), ss(cardHeight));
			int fill = module == selectedModule ? UiRenderer.withAlpha(accentColor, 86) : cardPanel;
			UiRenderer.drawPanel(drawContext, sx(cardX), sy(cardY), ss(cardWidth), ss(cardHeight), fill, hovered ? accentColor : 0xFF2F3732);
			renderModuleSafetyDot(drawContext, module, cardX + 16, cardY + 21);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, module.getName(), sx(cardX + 28), sy(cardY + 20), ss(cardWidth - 44), textColor);
			UiRenderer.drawWrappedText(drawContext, textRenderer, module.getDescription(), sx(cardX + 16), sy(cardY + 38), ss(cardWidth - 32), 3, UiRenderer.withAlpha(textColor, 150));
			UiRenderer.drawPill(drawContext, sx(cardX + 16), sy(cardY + 68), ss(92), ss(12), 0xFF223029);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, module.getCategory(), sx(cardX + 22), sy(cardY + 71), ss(80), UiRenderer.withAlpha(textColor, 185));
			UiRenderer.drawPill(drawContext, sx(cardX + 114), sy(cardY + 68), ss(72), ss(12), 0xFF30433A);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Pick Tab", sx(cardX + 124), sy(cardY + 71), ss(52), 0xFFF5F6F7);
			UiRenderer.drawPill(drawContext, sx(cardX + 16), sy(cardY + 82), ss(cardWidth - 32), ss(18), 0xFF242C27);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Right click for options / Ctrl + MMB hides", sx(cardX + 22), sy(cardY + 88), ss(cardWidth - 44), UiRenderer.withAlpha(textColor, 180));
			UiRenderer.drawPill(drawContext, sx(cardX + 16), sy(cardY + 102), ss(cardWidth - 32), ss(16), module.isEnabled() ? 0xFF1EB56A : 0xFFB93557);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, module.isEnabled() ? "ENABLED" : "DISABLED", sx(cardX + 71), sy(cardY + 107), ss(60), 0xFFF5F6F7);
		}
		drawContext.disableScissor();
		renderPanelScrollBar(drawContext, x + width - 10, y + 44, viewportHeight, moduleScroll, maxScroll, contentHeight, accentColor);
		renderTabPicker(drawContext, mouseX, mouseY, accentColor, textColor);
	}

	private void renderRightPreview(DrawContext drawContext, int textColor, int accentColor, int mutedPanel) {
		UiRenderer.drawPanel(drawContext, sx(706), sy(94), ss(214), ss(416), mutedPanel, 0xFF28312C);
		renderSafetyLegend(drawContext, textColor);
		if (activePanel == ScreenPanel.SETTINGS) {
			if (showHiddenModulesSettings) {
				renderHiddenModulePreview(drawContext, textColor, accentColor);
				return;
			}
			renderThemePresetPreview(drawContext, textColor);
			return;
		}
		if (selectedModule == null) {
		UiRenderer.drawText(drawContext, textRenderer, "Select a module", sx(724), sy(126), textColor);
			return;
		}

		renderModuleSafetyDot(drawContext, selectedModule, 724, 125);
		UiRenderer.drawText(drawContext, textRenderer, selectedModule.getName(), sx(736), sy(124), textColor);
		UiRenderer.drawWrappedText(drawContext, textRenderer, selectedModule.getDescription(), sx(724), sy(142), ss(178), 4, UiRenderer.withAlpha(textColor, 150));
		UiRenderer.drawText(drawContext, textRenderer, "Category", sx(724), sy(206), UiRenderer.withAlpha(textColor, 135));
		UiRenderer.drawPill(drawContext, sx(724), sy(218), ss(96), ss(18), 0xFF223029);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, selectedModule.getCategory(), sx(736), sy(224), ss(72), textColor);
		UiRenderer.drawPill(drawContext, sx(828), sy(218), ss(72), ss(18), accentColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Pick Tab", sx(842), sy(224), ss(46), 0xFF081217);

		UiRenderer.drawText(drawContext, textRenderer, "State", sx(724), sy(252), UiRenderer.withAlpha(textColor, 135));
		UiRenderer.drawPill(drawContext, sx(724), sy(264), ss(96), ss(18), selectedModule.isEnabled() ? 0xFF1EB56A : 0xFFB93557);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, selectedModule.isEnabled() ? "Enabled" : "Disabled", sx(746), sy(270), ss(52), 0xFFF5F6F7);

		UiRenderer.drawText(drawContext, textRenderer, "Keybind", sx(724), sy(298), UiRenderer.withAlpha(textColor, 135));
		UiRenderer.drawPill(drawContext, sx(724), sy(310), ss(132), ss(18), 0xFF223029);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, selectedModule.getBoundKeyName(), sx(736), sy(316), ss(108), textColor);

		UiRenderer.drawText(drawContext, textRenderer, "Tip", sx(724), sy(352), UiRenderer.withAlpha(textColor, 135));
		UiRenderer.drawWrappedText(drawContext, textRenderer, "Left click the bottom strip to toggle. Right click the card to open settings and keybinds.", sx(724), sy(366), ss(176), 5, UiRenderer.withAlpha(textColor, 155));
		UiRenderer.drawPill(drawContext, sx(724), sy(424), ss(176), ss(22), 0xFF223029);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Copy Tab Layout", sx(770), sy(431), ss(84), 0xFFF5F6F7);
		UiRenderer.drawPill(drawContext, sx(724), sy(454), ss(176), ss(22), accentColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Open HUD Editor", sx(772), sy(461), ss(100), 0xFF07131A);
	}

	private void renderSafetyLegend(DrawContext drawContext, int textColor) {
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Legend", sx(724), sy(106), ss(52), UiRenderer.withAlpha(textColor, 150));
		renderLegendEntry(drawContext, 780, 105, 0xFF38C172, "Safe", textColor);
		renderLegendEntry(drawContext, 826, 105, 0xFFF0C54A, "Risky", textColor);
		renderLegendEntry(drawContext, 878, 105, 0xFFE34B4B, "Bannable", textColor);
	}

	private void renderLegendEntry(DrawContext drawContext, int x, int y, int color, String label, int textColor) {
		int left = sx(x);
		int top = sy(y);
		int size = ss(7);
		drawContext.fill(left, top, left + size, top + size, color);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, label, sx(x + 11), sy(y - 1), ss(46), UiRenderer.withAlpha(textColor, 175));
	}

	private void renderModuleSafetyDot(DrawContext drawContext, Module module, int x, int y) {
		int color = switch (module.getSafetyLevel()) {
			case SAFE -> 0xFF38C172;
			case RISKY -> 0xFFF0C54A;
			case BANNABLE -> 0xFFE34B4B;
			case NONE -> 0;
		};
		if (color == 0) {
			return;
		}

		int size = ss(7);
		int left = sx(x);
		int top = sy(y);
		drawContext.fill(left, top, left + size, top + size, color);
	}

	private void renderHiddenModulePreview(DrawContext drawContext, int textColor, int accentColor) {
		List<Module> hiddenModules = hiddenModules();
		UiRenderer.drawText(drawContext, textRenderer, "Hidden Modules", sx(724), sy(124), textColor);
		UiRenderer.drawWrappedText(drawContext, textRenderer, "Ctrl + middle click a row on the left to restore it to its original category.", sx(724), sy(142), ss(178), 4, UiRenderer.withAlpha(textColor, 150));
		UiRenderer.drawPill(drawContext, sx(724), sy(198), ss(92), ss(18), 0xFF223029);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, hiddenModules.size() + " hidden", sx(744), sy(204), ss(52), textColor);

		if (selectedModule != null && selectedModule.isHidden()) {
			UiRenderer.drawText(drawContext, textRenderer, selectedModule.getName(), sx(724), sy(248), textColor);
			UiRenderer.drawWrappedText(drawContext, textRenderer, selectedModule.getDescription(), sx(724), sy(266), ss(178), 4, UiRenderer.withAlpha(textColor, 150));
			UiRenderer.drawText(drawContext, textRenderer, "Original Category", sx(724), sy(330), UiRenderer.withAlpha(textColor, 135));
			UiRenderer.drawPill(drawContext, sx(724), sy(342), ss(116), ss(18), accentColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, selectedModule.getCategory(), sx(736), sy(348), ss(92), 0xFF081217);
		} else {
			UiRenderer.drawWrappedText(drawContext, textRenderer, hiddenModules.isEmpty() ? "No modules are hidden right now." : "Pick a hidden module row on the left, then use Ctrl + middle click to bring it back.", sx(724), sy(248), ss(178), 5, UiRenderer.withAlpha(textColor, 150));
		}
	}

	private void renderThemePresetPreview(DrawContext drawContext, int textColor) {
		themePresetScroll = MathHelper.clamp(themePresetScroll, 0, maxThemePresetScroll());
		ThemePreset activePreset = ThemePresets.match(context.configManager().getConfig().theme);
		UiRenderer.drawText(drawContext, textRenderer, "Theme Presets", sx(724), sy(124), textColor);
		UiRenderer.drawWrappedText(
			drawContext,
			textRenderer,
			activePreset == null
				? "Pick a full preset here, then fine tune the sliders on the left if you want a custom mix."
				: "Current look: " + activePreset.name() + ". Click any preset card to swap the whole style.",
			sx(724),
			sy(142),
			ss(178),
			4,
			UiRenderer.withAlpha(textColor, 150)
		);

		int cardY = 192 - themePresetScroll;
		drawContext.enableScissor(sx(718), sy(190), sx(906), sy(500));
		for (ThemePreset preset : ThemePresets.all()) {
			boolean active = ThemePresets.matches(context.configManager().getConfig().theme, preset);
			int cardX = 724;
			int cardWidth = 178;
			int cardHeight = 48;
			int cardFill = UiRenderer.withAlpha(preset.panelColor(), active ? 236 : 214);
			UiRenderer.drawPanel(drawContext, sx(cardX), sy(cardY), ss(cardWidth), ss(cardHeight), cardFill, active ? preset.accentColor() : 0xFF2F3732);
			drawContext.fill(sx(cardX + 12), sy(cardY + 14), sx(cardX + 28), sy(cardY + 30), preset.backgroundColor());
			drawContext.fill(sx(cardX + 32), sy(cardY + 14), sx(cardX + 48), sy(cardY + 30), preset.accentColor());
			drawContext.fill(sx(cardX + 52), sy(cardY + 14), sx(cardX + 68), sy(cardY + 30), preset.textColor());
			UiRenderer.drawTrimmedText(drawContext, textRenderer, preset.name(), sx(cardX + 78), sy(cardY + 12), ss(88), preset.textColor());
			UiRenderer.drawTrimmedText(drawContext, textRenderer, preset.vibe(), sx(cardX + 78), sy(cardY + 24), ss(88), UiRenderer.withAlpha(preset.textColor(), 178));
			cardY += 58;
		}
		drawContext.disableScissor();
	}

	private void renderKeybinds(DrawContext drawContext, int x, int y, int width, int height, int textColor, int accentColor, int mutedPanel) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(height), mutedPanel, 0xFF283029);
		UiRenderer.drawText(drawContext, textRenderer, "Keybinds", sx(x + 18), sy(y + 18), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "This tab now includes the client binds plus the full Minecraft controls list. Click a bind to rebind it, then press a keyboard key or mouse button.", sx(x + 18), sy(y + 34), ss(width - 36), UiRenderer.withAlpha(textColor, 145));

		List<KeybindEntry> entries = keybindEntries();
		int viewportTop = y + 70;
		int viewportHeight = height - 84;
		int contentHeight = keybindContentHeight(entries);
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		keybindScroll = MathHelper.clamp(keybindScroll, 0, maxScroll);

		drawContext.enableScissor(sx(x + 12), sy(viewportTop), sx(x + width - 12), sy(viewportTop + viewportHeight));
		int rowY = viewportTop - keybindScroll;
		String lastCategory = "";
		for (KeybindEntry entry : entries) {
			if (!entry.category().equals(lastCategory)) {
				UiRenderer.drawTrimmedText(drawContext, textRenderer, entry.category(), sx(x + 18), sy(rowY + 2), ss(width - 48), UiRenderer.withAlpha(textColor, 150));
				rowY += 22;
				lastCategory = entry.category();
			}

			boolean binding = entry.binding();
			UiRenderer.drawPanel(drawContext, sx(x + 18), sy(rowY), ss(width - 36), ss(34), 0xFF202824, 0xFF2F3732);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, entry.label(), sx(x + 32), sy(rowY + 12), ss(width - 332), textColor);
			UiRenderer.drawPill(drawContext, sx(x + width - 244), sy(rowY + 7), ss(62), ss(20), 0xFF5D6268);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Unbind", sx(x + width - 228), sy(rowY + 13), ss(34), 0xFFF5F6F7);
			UiRenderer.drawPill(drawContext, sx(x + width - 174), sy(rowY + 7), ss(156), ss(20), binding ? accentColor : 0xFF25302A);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, binding ? "Press a key..." : entry.keyName(), sx(x + width - 162), sy(rowY + 13), ss(132), binding ? 0xFF081217 : textColor);
			rowY += 42;
		}
		drawContext.disableScissor();
		renderPanelScrollBar(drawContext, x + width - 10, viewportTop, viewportHeight, keybindScroll, maxScroll, contentHeight, accentColor);
	}

	private void renderGlobalBindRow(DrawContext drawContext, int x, int y, int width, String label, boolean binding, String keyText, int textColor, int accentColor) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(34), 0xFF202824, 0xFF2F3732);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, label, sx(x + 14), sy(y + 12), ss(width - 216), textColor);
		UiRenderer.drawPill(drawContext, sx(x + width - 174), sy(y + 7), ss(156), ss(20), binding ? accentColor : 0xFF25302A);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, binding ? "Press a key..." : keyText, sx(x + width - 162), sy(y + 13), ss(132), binding ? 0xFF081217 : textColor);
	}

	private void renderSettings(DrawContext drawContext, int x, int y, int width, int height, int textColor, int accentColor, int mutedPanel) {
		settingsScroll = MathHelper.clamp(settingsScroll, 0, maxSettingsScroll(height));
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(height), mutedPanel, 0xFF283029);
		UiRenderer.drawText(drawContext, textRenderer, showHiddenModulesSettings ? "Hidden Modules" : "Client Settings", sx(x + 18), sy(y + 18), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, showHiddenModulesSettings ? "Ctrl + middle click any row below to restore that module to its original category." : "Theme presets live on the right. Use the controls here to fine tune the active look.", sx(x + 18), sy(y + 34), ss(width - 120), UiRenderer.withAlpha(textColor, 145));
		if (!showHiddenModulesSettings) {
			UiRenderer.drawTrimmedText(drawContext, textRenderer, VipAccessManager.getStatusText(), sx(x + 18), sy(y + 46), ss(width - 120), 0xFF9BEA87);
		}
		renderSettingsModeButton(drawContext, x + width - 74, y + 16, 56, textColor, accentColor);
		int contentTop = y + 54;
		int contentHeight = settingsViewportHeight(height);
		drawContext.enableScissor(sx(x + 12), sy(contentTop), sx(x + width - 12), sy(contentTop + contentHeight));
		if (showHiddenModulesSettings) {
			renderHiddenModulesSettings(drawContext, x, contentTop, width, textColor, accentColor);
		} else {
			int rowY = contentTop - settingsScroll;
			rowY = renderSlider(drawContext, x + 18, rowY, width - 36, "UI Transparency", context.configManager().getConfig().theme.uiTransparency, 0.35F, 1.00F, textColor, accentColor);
			rowY = renderSlider(drawContext, x + 18, rowY, width - 36, "UI Scale", uiScale(), 1.00F, 1.40F, textColor, accentColor);
			rowY = renderSlider(drawContext, x + 18, rowY, width - 36, "Overlay Scale", context.configManager().getConfig().layout.globalScale, 0.70F, 1.80F, textColor, accentColor);
			renderColorRow(drawContext, x + 18, rowY, width - 36, "background", "Background", context.configManager().getConfig().theme.backgroundColor, textColor);
			rowY += 34;
			renderColorRow(drawContext, x + 18, rowY, width - 36, "accent", "Accent", context.configManager().getConfig().theme.accentColor, textColor);
			rowY += 34;
			renderColorRow(drawContext, x + 18, rowY, width - 36, "text", "Text", context.configManager().getConfig().theme.textColor, textColor);
			rowY += 42;
			int activeColor = getThemeColor(selectedColorTarget);
			rowY = renderSlider(drawContext, x + 18, rowY, width - 36, "Red", red(activeColor), 0.0F, 255.0F, textColor, 0xFFF87171);
			rowY = renderSlider(drawContext, x + 18, rowY, width - 36, "Green", green(activeColor), 0.0F, 255.0F, textColor, 0xFF4ADE80);
			rowY = renderSlider(drawContext, x + 18, rowY, width - 36, "Blue", blue(activeColor), 0.0F, 255.0F, textColor, 0xFF60A5FA);
			int toggleWidth = (width - 48) / 2;
			renderSettingsToggle(drawContext, x + 18, rowY, toggleWidth, "HUD Visibility", context.configManager().getConfig().layout.showOverlays, textColor);
			renderSettingsToggle(drawContext, x + 24 + toggleWidth, rowY, toggleWidth, "GUI Stars", context.configManager().getConfig().theme.starsEnabled, textColor);
			rowY += 42;
			renderSettingsToggle(drawContext, x + 18, rowY, width - 36, "Tropical Water", context.configManager().getConfig().theme.tropicalOverlayEnabled, textColor);
		}
		drawContext.disableScissor();
		renderScrollBar(drawContext, x + width - 10, contentTop, contentHeight, settingsScroll, maxSettingsScroll(height), accentColor);
	}

	private void renderSettingsModeButton(DrawContext drawContext, int x, int y, int width, int textColor, int accentColor) {
		UiRenderer.drawPill(drawContext, sx(x), sy(y), ss(width), ss(20), showHiddenModulesSettings ? accentColor : 0xFF223029);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "H-M", sx(x + 18), sy(y + 6), ss(width - 36), showHiddenModulesSettings ? 0xFF081217 : textColor);
	}

	private void renderHiddenModulesSettings(DrawContext drawContext, int x, int contentTop, int width, int textColor, int accentColor) {
		List<Module> hiddenModules = hiddenModules();
		if (hiddenModules.isEmpty()) {
			UiRenderer.drawPanel(drawContext, sx(x + 18), sy(contentTop - settingsScroll), ss(width - 36), ss(44), 0xFF202824, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "No hidden modules yet. Ctrl + middle click a module card in Mods to send it here.", sx(x + 30), sy(contentTop + 14 - settingsScroll), ss(width - 60), UiRenderer.withAlpha(textColor, 170));
			return;
		}

		int rowY = contentTop - settingsScroll;
		for (Module module : hiddenModules) {
			boolean selected = module == selectedModule;
			int fill = selected ? UiRenderer.withAlpha(accentColor, 88) : 0xFF202824;
			UiRenderer.drawPanel(drawContext, sx(x + 18), sy(rowY), ss(width - 36), ss(50), fill, selected ? accentColor : 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, module.getName(), sx(x + 30), sy(rowY + 10), ss(width - 240), textColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, module.getCategory(), sx(x + 30), sy(rowY + 24), ss(120), UiRenderer.withAlpha(textColor, 155));
			UiRenderer.drawTrimmedText(drawContext, textRenderer, module.isEnabled() ? "Running" : "Idle", sx(x + width - 182), sy(rowY + 10), ss(56), module.isEnabled() ? 0xFF8AF5A7 : UiRenderer.withAlpha(textColor, 150));
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Ctrl + MMB restore", sx(x + width - 182), sy(rowY + 24), ss(126), UiRenderer.withAlpha(textColor, 170));
			rowY += 58;
		}
	}

	private int renderSlider(DrawContext drawContext, int x, int y, int width, String name, float value, float min, float max, int textColor, int accentColor) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(34), 0xFF202824, 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, name, sx(x + 12), sy(y + 8), ss(width - 120), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, String.format(Locale.US, "%.2f", value), sx(x + width - 64), sy(y + 8), ss(48), UiRenderer.withAlpha(textColor, 170));
		int barX = sx(sliderBarLocalX(x));
		int barY = sy(sliderBarLocalY(y));
		int barWidth = ss(sliderBarLocalWidth(width));
		int barHeight = ss(sliderBarLocalHeight());
		int knobWidth = ss(8);
		int knobHeight = ss(12);
		UiRenderer.fillRoundedRect(drawContext, barX, barY, barWidth, barHeight, 3, 0xFF232B29);
		int fillWidth = (int) (((value - min) / (max - min)) * barWidth);
		UiRenderer.fillRoundedRect(drawContext, barX, barY, Math.max(ss(6), fillWidth), barHeight, 3, accentColor);
		int knobX = barX + MathHelper.clamp(fillWidth - (knobWidth / 2), 0, Math.max(0, barWidth - knobWidth));
		UiRenderer.fillRoundedRect(drawContext, knobX, barY - ss(2), knobWidth, knobHeight, 4, 0xFFF5F6F7);
		return y + 42;
	}

	private void renderColorRow(DrawContext drawContext, int x, int y, int width, String id, String label, int color, int textColor) {
		boolean active = selectedColorTarget.equals(id);
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(26), active ? UiRenderer.withAlpha(color, 120) : 0xFF202824, active ? color : 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, label, sx(x + 12), sy(y + 9), ss(width - 100), textColor);
		drawContext.fill(sx(x + width - 34), sy(y + 5), sx(x + width - 12), sy(y + 21), color);
	}

	private void renderSettingsToggle(DrawContext drawContext, int x, int y, int width, String label, boolean enabled, int textColor) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(34), 0xFF202824, 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, label, sx(x + 12), sy(y + 12), ss(width - 124), textColor);
		UiRenderer.drawPill(drawContext, sx(x + width - 98), sy(y + 7), ss(80), ss(20), enabled ? 0xFF1EB56A : 0xFFB93557);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, enabled ? "Enabled" : "Disabled", sx(x + width - 84), sy(y + 13), ss(52), 0xFFF5F6F7);
	}

	private void renderHudPanel(DrawContext drawContext, int x, int y, int width, int height, int textColor, int accentColor, int mutedPanel) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(height), mutedPanel, 0xFF283029);
		UiRenderer.drawText(drawContext, textRenderer, "HUD Options", sx(x + 18), sy(y + 18), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Toggle individual HUDs here, then open the editor to move them around.", sx(x + 18), sy(y + 34), ss(width - 36), UiRenderer.withAlpha(textColor, 145));

		int viewportTop = y + 72;
		int viewportHeight = height - 124;
		int contentHeight = Math.max(viewportHeight, visibleHudElements().size() * 38);
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		hudScroll = MathHelper.clamp(hudScroll, 0, maxScroll);

		drawContext.enableScissor(sx(x + 12), sy(viewportTop), sx(x + width - 12), sy(viewportTop + viewportHeight));
		int rowY = viewportTop - hudScroll;
		for (HudElement element : visibleHudElements()) {
			UiRenderer.drawPanel(drawContext, sx(x + 18), sy(rowY), ss(width - 36), ss(30), 0xFF202824, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, element.getName(), sx(x + 30), sy(rowY + 11), ss(220), textColor);
			UiRenderer.drawPill(drawContext, sx(x + width - 126), sy(rowY + 5), ss(90), ss(20), element.isEnabled() ? 0xFF1EB56A : 0xFFB93557);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, element.isEnabled() ? "Enabled" : "Disabled", sx(x + width - 101), sy(rowY + 11), ss(54), 0xFFF5F6F7);
			rowY += 38;
		}
		drawContext.disableScissor();

		UiRenderer.drawPill(drawContext, sx(x + 18), sy(y + height - 40), ss(148), ss(22), accentColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Open HUD Editor", sx(x + 56), sy(y + height - 33), ss(98), 0xFF07131A);
		renderPanelScrollBar(drawContext, x + width - 10, viewportTop, viewportHeight, hudScroll, maxScroll, contentHeight, accentColor);
	}

	private void renderProfiles(DrawContext drawContext, int x, int y, int width, int height, int textColor, int accentColor, int mutedPanel) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(height), mutedPanel, 0xFF283029);
		UiRenderer.drawText(drawContext, textRenderer, "Profiles", sx(x + 18), sy(y + 18), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Save named loadouts with module states, settings, HUD positions, and keybinds.", sx(x + 18), sy(y + 34), ss(width - 36), UiRenderer.withAlpha(textColor, 145));

		UiRenderer.drawPanel(drawContext, sx(x + 18), sy(y + 48), ss(250), ss(52), 0xFF202824, 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Profile Name", sx(x + 30), sy(y + 58), ss(120), textColor);

		UiRenderer.drawPill(drawContext, sx(x + 282), sy(y + 54), ss(112), ss(20), accentColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Save Current", sx(x + 314), sy(y + 60), ss(58), 0xFF07131A);
		UiRenderer.drawPill(drawContext, sx(x + 404), sy(y + 54), ss(112), ss(20), 0xFF2B7FFF);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Load Selected", sx(x + 435), sy(y + 60), ss(64), 0xFFF6F9FC);
		UiRenderer.drawPill(drawContext, sx(x + 526), sy(y + 54), ss(112), ss(20), 0xFFAA2E57);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Delete Selected", sx(x + 553), sy(y + 60), ss(72), 0xFFF7F8F9);

		UiRenderer.drawPanel(drawContext, sx(x + 18), sy(y + 116), ss(width - 36), ss(34), 0xFF202824, 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Auto Load On Server Join", sx(x + 30), sy(y + 128), ss(220), textColor);
		UiRenderer.drawPill(drawContext, sx(x + width - 126), sy(y + 123), ss(90), ss(20), context.profileManager().isAutoLoadEnabled() ? 0xFF1EB56A : 0xFFB93557);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, context.profileManager().isAutoLoadEnabled() ? "ON" : "OFF", sx(x + width - 95), sy(y + 129), ss(32), 0xFFF5F6F7);

		int listTop = y + 160;
		int listHeight = height - 178;
		UiRenderer.drawPanel(drawContext, sx(x + 18), sy(listTop), ss(width - 36), ss(listHeight), 0xFF1C2420, 0xFF313B34);
		List<ProfileEntry> profiles = context.profileManager().getProfiles();
		if (profiles.isEmpty()) {
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "No profiles saved yet.", sx(x + 34), sy(listTop + 12), ss(width - 68), UiRenderer.withAlpha(textColor, 150));
			return;
		}

		int contentHeight = Math.max(listHeight - 12, profiles.size() * 38);
		int maxScroll = Math.max(0, contentHeight - (listHeight - 12));
		profileScroll = MathHelper.clamp(profileScroll, 0, maxScroll);
		String autoLoadProfile = context.profileManager().getAutoLoadProfileName();
		drawContext.enableScissor(sx(x + 18), sy(listTop), sx(x + width - 18), sy(listTop + listHeight));
		int rowY = listTop + 12 - profileScroll;
		for (ProfileEntry profile : profiles) {
			boolean selected = profile.name.equals(selectedProfileName);
			boolean autoLoad = profile.name.equals(autoLoadProfile);
			UiRenderer.drawPanel(drawContext, sx(x + 30), sy(rowY), ss(width - 60), ss(30), selected ? UiRenderer.withAlpha(accentColor, 90) : 0xFF202824, selected ? accentColor : 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, profile.name, sx(x + 42), sy(rowY + 10), ss(240), textColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, autoLoad ? "Auto-load target" : "Click to select", sx(x + 290), sy(rowY + 10), ss(140), UiRenderer.withAlpha(textColor, 145));
			if (context.profileManager().isAutoLoadEnabled()) {
				UiRenderer.drawPill(drawContext, sx(x + width - 178), sy(rowY + 5), ss(130), ss(20), autoLoad ? 0xFF1EB56A : 0xFF25302A);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, autoLoad ? "Selected for join" : "Use for join", sx(x + width - 145), sy(rowY + 11), ss(90), autoLoad ? 0xFF07131A : textColor);
			}
			rowY += 38;
		}
		drawContext.disableScissor();
		renderPanelScrollBar(drawContext, x + width - 10, listTop + 6, listHeight - 12, profileScroll, maxScroll, contentHeight, accentColor);
	}

	private void renderServers(DrawContext drawContext, int x, int y, int width, int height, int textColor, int accentColor, int mutedPanel) {
		UiRenderer.drawPanel(drawContext, sx(x), sy(y), ss(width), ss(height), mutedPanel, 0xFF283029);
		UiRenderer.drawText(drawContext, textRenderer, "Servers", sx(x + 18), sy(y + 18), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Choose which server tab set the mods panel should use. Global shows mods that apply everywhere.", sx(x + 18), sy(y + 34), ss(width - 36), UiRenderer.withAlpha(textColor, 145));

		String currentServerLabel = currentServerSelectionLabel();
		UiRenderer.drawPanel(drawContext, sx(x + 18), sy(y + 54), ss(width - 36), ss(52), 0xFF202824, 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Active Server Filter", sx(x + 30), sy(y + 64), ss(220), textColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, currentServerLabel, sx(x + 30), sy(y + 80), ss(width - 180), UiRenderer.withAlpha(textColor, 160));
		UiRenderer.drawPill(drawContext, sx(x + width - 154), sy(y + 68), ss(118), ss(20), accentColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Use Current", sx(x + width - 122), sy(y + 74), ss(62), 0xFF07131A);

		int listTop = y + 126;
		int rowY = listTop;
		List<String> serverKeys = availableServerKeys();
		for (String serverKey : serverKeys) {
			String label = serverKey.isBlank() ? "Global" : serverLabel(serverKey);
			boolean selected = serverKey.equals(selectedServerKey);
			UiRenderer.drawPanel(drawContext, sx(x + 18), sy(rowY), ss(width - 36), ss(34), selected ? UiRenderer.withAlpha(accentColor, 90) : 0xFF202824, selected ? accentColor : 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, label, sx(x + 32), sy(rowY + 12), ss(width - 160), textColor);
			String type = serverKey.isBlank() ? "All servers" : "Server-specific tabs";
			UiRenderer.drawTrimmedText(drawContext, textRenderer, type, sx(x + width - 180), sy(rowY + 12), ss(128), UiRenderer.withAlpha(textColor, 145));
			rowY += 42;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && beginScrollDrag(mouseX, mouseY)) {
			return true;
		}
		if (bindingGuiKey) {
			context.openGuiKey().setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
			KeyBinding.updateKeysByCode();
			MinecraftClient.getInstance().options.write();
			bindingGuiKey = false;
			context.configManager().save(context);
			return true;
		}
		if (bindingPanicKey) {
			context.panicKey().setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
			KeyBinding.updateKeysByCode();
			MinecraftClient.getInstance().options.write();
			bindingPanicKey = false;
			context.configManager().save(context);
			return true;
		}
		if (bindingVanillaKey != null) {
			bindingVanillaKey.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
			KeyBinding.updateKeysByCode();
			MinecraftClient.getInstance().options.write();
			bindingVanillaKey = null;
			context.configManager().save(context);
			return true;
		}
		if (button == 0) {
			if (handleTabPickerClick(mouseX, mouseY)) {
				return true;
			}
			if (handleTopNavigation(mouseX, mouseY)) {
				return true;
			}
			switch (activePanel) {
				case MODULES -> {
					if (handleModuleClick(mouseX, mouseY, button)) {
						return true;
					}
					if (selectedModule != null && UiRenderer.isHovered(mouseX, mouseY, sx(828), sy(218), ss(72), ss(18))) {
						openTabPicker(selectedModule, 828, 238);
						return true;
					}
					if (UiRenderer.isHovered(mouseX, mouseY, sx(724), sy(424), ss(176), ss(22))) {
						copyModuleLayoutToClipboard();
						return true;
					}
					if (UiRenderer.isHovered(mouseX, mouseY, sx(724), sy(454), ss(176), ss(22))) {
						KuzayClientModClient.openHudEditor();
						return true;
					}
				}
				case KEYBINDS -> {
					if (handleKeybindClick(mouseX, mouseY, 20, 54, 670)) {
						return true;
					}
				}
				case SETTINGS -> {
					if (handleSettingsClick(mouseX, mouseY, button, 20, 54, 670, 456) || handleThemePresetClick(mouseX, mouseY)) {
						return true;
					}
				}
				case HUD -> {
					if (handleHudClick(mouseX, mouseY, 20, 54, 670, 456)) {
						return true;
					}
				}
				case PROFILES -> {
					if (handleProfilesClick(mouseX, mouseY, 20, 54, 670, 456)) {
						return true;
					}
				}
				case SERVERS -> {
					if (handleServersClick(mouseX, mouseY, 20, 54, 670, 456)) {
						return true;
					}
				}
				default -> {
				}
			}
		}

		if (button == 1 && activePanel == ScreenPanel.MODULES) {
			if (handleModuleClick(mouseX, mouseY, button)) {
				return true;
			}
		}

		if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
			if (activePanel == ScreenPanel.MODULES && handleModuleClick(mouseX, mouseY, button)) {
				return true;
			}
			if (activePanel == ScreenPanel.SETTINGS && handleSettingsClick(mouseX, mouseY, button, 20, 54, 670, 456)) {
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleKeybindClick(double mouseX, double mouseY, int x, int y, int width) {
		List<KeybindEntry> entries = keybindEntries();
		int viewportTop = y + 70;
		int rowY = viewportTop - keybindScroll;
		String lastCategory = "";
		for (KeybindEntry entry : entries) {
			if (!entry.category().equals(lastCategory)) {
				rowY += 22;
				lastCategory = entry.category();
			}

			if (UiRenderer.isHovered(mouseX, mouseY, sx(x + width - 244), sy(rowY + 7), ss(62), ss(20))) {
				switch (entry.type()) {
					case OPEN_GUI -> context.openGuiKey().setBoundKey(InputUtil.UNKNOWN_KEY);
					case PANIC -> context.panicKey().setBoundKey(InputUtil.UNKNOWN_KEY);
					case VANILLA -> entry.keyBinding().setBoundKey(InputUtil.UNKNOWN_KEY);
				}
				KeyBinding.updateKeysByCode();
				MinecraftClient.getInstance().options.write();
				bindingGuiKey = false;
				bindingPanicKey = false;
				bindingVanillaKey = null;
				context.configManager().save(context);
				return true;
			}
			if (UiRenderer.isHovered(mouseX, mouseY, sx(x + width - 174), sy(rowY + 7), ss(156), ss(20))) {
				bindingGuiKey = false;
				bindingPanicKey = false;
				bindingVanillaKey = null;
				switch (entry.type()) {
					case OPEN_GUI -> bindingGuiKey = true;
					case PANIC -> bindingPanicKey = true;
					case VANILLA -> bindingVanillaKey = entry.keyBinding();
				}
				return true;
			}
			rowY += 42;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (button == 0 && scrollDragTarget != null) {
			updateScrollDrag(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			scrollDragTarget = null;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private boolean handleTopNavigation(double mouseX, double mouseY) {
		int x = 20;
		for (ScreenPanel panel : ScreenPanel.values()) {
			String label = switch (panel) {
				case MODULES -> "MODS";
				case KEYBINDS -> "KEYBINDS";
				case SETTINGS -> "SETTINGS";
				case HUD -> "HUD";
				case PROFILES -> "PROFILES";
				case SERVERS -> "SERVERS";
			};
			int width = Math.max(90, textRenderer.getWidth(label) + 34);
			if (UiRenderer.isHovered(mouseX, mouseY, sx(x), sy(22), ss(width), ss(20))) {
				activePanel = panel;
				if (panel == ScreenPanel.MODULES && (selectedModule == null || selectedModule.isHidden())) {
					List<Module> visibleModules = visibleModules();
					selectedModule = visibleModules.isEmpty() ? null : visibleModules.get(0);
				}
				if (searchField != null) {
					boolean focusSearch = panel == ScreenPanel.MODULES || panel == ScreenPanel.KEYBINDS;
					searchField.setFocused(focusSearch);
					if (focusSearch) {
						setFocused(searchField);
					}
				}
				return true;
			}
			x += width + 10;
		}
		return false;
	}

	private boolean handleModuleClick(double mouseX, double mouseY, int button) {
		int x = 20;
		int y = 54;
		int contentX = x + 16;
		int contentY = y + 46 - moduleScroll;
		int cardWidth = 202;
		int cardHeight = 126;
		int gap = 12;

		int categoryX = x + 16;
		for (ClientTab category : categories()) {
			int pillWidth = Math.max(54, textRenderer.getWidth(category.label()) + 24);
			if (UiRenderer.isHovered(mouseX, mouseY, sx(categoryX), sy(y + 12), ss(pillWidth), ss(18))) {
				selectTab(category);
				moduleScroll = 0;
				return true;
			}
			categoryX += pillWidth + 8;
		}

		List<Module> modules = filteredModules();
		for (int index = 0; index < modules.size(); index++) {
			Module module = modules.get(index);
			int column = index % 3;
			int row = index / 3;
			int cardX = contentX + (column * (cardWidth + gap));
			int cardY = contentY + (row * (cardHeight + gap));
			if (UiRenderer.isHovered(mouseX, mouseY, sx(cardX), sy(cardY), ss(cardWidth), ss(cardHeight))) {
				selectedModule = module;
				if (UiRenderer.isHovered(mouseX, mouseY, sx(cardX + 114), sy(cardY + 68), ss(72), ss(12))) {
					openTabPicker(module, cardX + 114, cardY + 82);
					return true;
				}
				if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && hasControlDown()) {
					setModuleHidden(module, true);
					return true;
				}
				if (button == 1) {
					client.setScreen(new ModuleSettingsScreen(context, this, module));
					return true;
				}
				if (UiRenderer.isHovered(mouseX, mouseY, sx(cardX + 16), sy(cardY + 102), ss(cardWidth - 32), ss(16))) {
					module.toggle();
					context.configManager().save(context);
				}
				return true;
			}
		}
		return false;
	}

	private boolean handleSettingsClick(double mouseX, double mouseY, int button, int x, int y, int width, int height) {
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + width - 74), sy(y + 16), ss(56), ss(20))) {
			showHiddenModulesSettings = !showHiddenModulesSettings;
			settingsScroll = 0;
			return true;
		}

		int contentTop = y + 54;
		int contentHeight = settingsViewportHeight(height);
		if (!UiRenderer.isHovered(mouseX, mouseY, sx(x + 12), sy(contentTop), ss(width - 24), ss(contentHeight))) {
			return false;
		}

		if (showHiddenModulesSettings) {
			return handleHiddenModulesClick(mouseX, mouseY, button, x, contentTop, width);
		}

		int toggleWidth = (width - 48) / 2;
		int rowY = contentTop - settingsScroll;
		if (applySliderClick(mouseX, mouseY, x + 18, rowY, width - 36, 0.35F, 1.00F, value -> context.configManager().getConfig().theme.uiTransparency = value)) return true;
		rowY += 42;
		if (applySliderClick(mouseX, mouseY, x + 18, rowY, width - 36, 1.00F, 1.40F, value -> context.configManager().getConfig().theme.uiScale = value)) return true;
		rowY += 42;
		if (applySliderClick(mouseX, mouseY, x + 18, rowY, width - 36, 0.70F, 1.80F, value -> context.configManager().getConfig().layout.globalScale = value)) return true;
		rowY += 42;
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(width - 36), ss(26))) {
			selectedColorTarget = "background";
			return true;
		}
		rowY += 34;
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(width - 36), ss(26))) {
			selectedColorTarget = "accent";
			return true;
		}
		rowY += 34;
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(width - 36), ss(26))) {
			selectedColorTarget = "text";
			return true;
		}
		rowY += 42;
		if (applySliderClick(mouseX, mouseY, x + 18, rowY, width - 36, 0.0F, 255.0F, value -> setThemeColorChannel(selectedColorTarget, 16, (int) value))) return true;
		rowY += 42;
		if (applySliderClick(mouseX, mouseY, x + 18, rowY, width - 36, 0.0F, 255.0F, value -> setThemeColorChannel(selectedColorTarget, 8, (int) value))) return true;
		rowY += 42;
		if (applySliderClick(mouseX, mouseY, x + 18, rowY, width - 36, 0.0F, 255.0F, value -> setThemeColorChannel(selectedColorTarget, 0, (int) value))) return true;
		rowY += 42;
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(toggleWidth), ss(34))) {
			context.configManager().getConfig().layout.showOverlays = !context.configManager().getConfig().layout.showOverlays;
			context.configManager().save(context);
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 24 + toggleWidth), sy(rowY), ss(toggleWidth), ss(34))) {
			context.configManager().getConfig().theme.starsEnabled = !context.configManager().getConfig().theme.starsEnabled;
			context.configManager().save(context);
			return true;
		}
		rowY += 42;
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(width - 36), ss(34))) {
			context.configManager().getConfig().theme.tropicalOverlayEnabled = !context.configManager().getConfig().theme.tropicalOverlayEnabled;
			context.configManager().save(context);
			return true;
		}
		return false;
	}

	private boolean handleHiddenModulesClick(double mouseX, double mouseY, int button, int x, int contentTop, int width) {
		int rowY = contentTop - settingsScroll;
		for (Module module : hiddenModules()) {
			if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(width - 36), ss(50))) {
				selectedModule = module;
				if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && hasControlDown()) {
					setModuleHidden(module, false);
				}
				return true;
			}
			rowY += 58;
		}
		return false;
	}

	private boolean handleThemePresetClick(double mouseX, double mouseY) {
		if (showHiddenModulesSettings) {
			return false;
		}
		int cardX = 724;
		int cardY = 192 - themePresetScroll;
		int cardWidth = 178;
		int cardHeight = 48;
		for (ThemePreset preset : ThemePresets.all()) {
			if (UiRenderer.isHovered(mouseX, mouseY, sx(cardX), sy(cardY), ss(cardWidth), ss(cardHeight))) {
				preset.applyTo(context.configManager().getConfig().theme);
				context.configManager().save(context);
				return true;
			}
			cardY += 58;
		}
		return false;
	}

	private boolean handleHudClick(double mouseX, double mouseY, int x, int y, int width, int height) {
		int rowY = y + 72 - hudScroll;
		for (HudElement element : visibleHudElements()) {
			if (UiRenderer.isHovered(mouseX, mouseY, sx(x + width - 126), sy(rowY + 5), ss(90), ss(20))) {
				element.setEnabled(!element.isEnabled());
				context.hudManager().save();
				return true;
			}
			rowY += 38;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(y + height - 40), ss(148), ss(22))) {
			KuzayClientModClient.openHudEditor();
			return true;
		}
		return false;
	}

	private boolean handleProfilesClick(double mouseX, double mouseY, int x, int y, int width, int height) {
		String profileName = profileNameField == null ? "" : profileNameField.getText().trim();
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 282), sy(y + 54), ss(112), ss(20))) {
			if (context.profileManager().saveProfile(profileName, context)) {
				selectedProfileName = profileName;
			}
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 404), sy(y + 54), ss(112), ss(20))) {
			if (context.profileManager().loadProfile(selectedProfileName, context)) {
				if (profileNameField != null) {
					profileNameField.setText(selectedProfileName);
				}
			}
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 526), sy(y + 54), ss(112), ss(20))) {
			if (context.profileManager().deleteProfile(selectedProfileName)) {
				selectedProfileName = "";
				if (profileNameField != null) {
					profileNameField.setText("");
				}
				List<ProfileEntry> profiles = context.profileManager().getProfiles();
				if (!profiles.isEmpty()) {
					selectedProfileName = profiles.get(0).name;
					profileNameField.setText(selectedProfileName);
				}
			}
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + width - 126), sy(y + 123), ss(90), ss(20))) {
			context.profileManager().setAutoLoadEnabled(!context.profileManager().isAutoLoadEnabled());
			return true;
		}

		int rowY = y + 172 - profileScroll;
		for (ProfileEntry profile : context.profileManager().getProfiles()) {
			if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 30), sy(rowY), ss(width - 60), ss(30))) {
				selectedProfileName = profile.name;
				if (profileNameField != null) {
					profileNameField.setText(profile.name);
				}
				if (context.profileManager().isAutoLoadEnabled()) {
					context.profileManager().setAutoLoadProfileName(profile.name);
				}
				return true;
			}
			rowY += 38;
		}

		return false;
	}

	private boolean applySliderClick(double mouseX, double mouseY, int x, int y, int width, float min, float max, SliderConsumer consumer) {
		int barX = sx(sliderBarLocalX(x));
		int barY = sy(sliderBarLocalY(y) - 2);
		int barWidth = ss(sliderBarLocalWidth(width));
		int barHeight = ss(sliderBarLocalHeight() + 4);
		if (!UiRenderer.isHovered(mouseX, mouseY, barX, barY, barWidth, barHeight)) {
			return false;
		}
		float percent = MathHelper.clamp((float) ((mouseX - barX) / Math.max(1, barWidth)), 0.0F, 1.0F);
		float value = min + (max - min) * percent;
		consumer.accept((float) (Math.round(value * 100.0F) / 100.0F));
		context.configManager().save(context);
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (activePanel == ScreenPanel.MODULES && UiRenderer.isHovered(mouseX, mouseY, sx(34), sy(110), ss(642), ss(386))) {
			moduleScroll = MathHelper.clamp(moduleScroll - (int) (verticalAmount * 24.0), 0, maxModuleScroll());
			return true;
		}
		if (activePanel == ScreenPanel.KEYBINDS && UiRenderer.isHovered(mouseX, mouseY, sx(32), sy(124), ss(646), ss(386))) {
			keybindScroll = MathHelper.clamp(keybindScroll - (int) (verticalAmount * 24.0), 0, maxKeybindScroll());
			return true;
		}
		if (activePanel == ScreenPanel.SETTINGS && !showHiddenModulesSettings && UiRenderer.isHovered(mouseX, mouseY, sx(706), sy(94), ss(214), ss(416))) {
			themePresetScroll = MathHelper.clamp(themePresetScroll - (int) (verticalAmount * 24.0), 0, maxThemePresetScroll());
			return true;
		}
		if (activePanel == ScreenPanel.SETTINGS && UiRenderer.isHovered(mouseX, mouseY, sx(32), sy(108), ss(646), ss(settingsViewportHeight(456)))) {
			settingsScroll = MathHelper.clamp(settingsScroll - (int) (verticalAmount * 24.0), 0, maxSettingsScroll(456));
			return true;
		}
		if (activePanel == ScreenPanel.HUD && UiRenderer.isHovered(mouseX, mouseY, sx(32), sy(126), ss(646), ss(332))) {
			hudScroll = MathHelper.clamp(hudScroll - (int) (verticalAmount * 24.0), 0, maxHudScroll());
			return true;
		}
		if (activePanel == ScreenPanel.PROFILES && UiRenderer.isHovered(mouseX, mouseY, sx(38), sy(214), ss(634), ss(278))) {
			profileScroll = MathHelper.clamp(profileScroll - (int) (verticalAmount * 24.0), 0, maxProfileScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingGuiKey) {
			context.openGuiKey().setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
			KeyBinding.updateKeysByCode();
			MinecraftClient.getInstance().options.write();
			bindingGuiKey = false;
			context.configManager().save(context);
			return true;
		}
		if (bindingPanicKey) {
			context.panicKey().setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
			KeyBinding.updateKeysByCode();
			MinecraftClient.getInstance().options.write();
			bindingPanicKey = false;
			context.configManager().save(context);
			return true;
		}
		if (bindingVanillaKey != null) {
			bindingVanillaKey.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
			KeyBinding.updateKeysByCode();
			MinecraftClient.getInstance().options.write();
			bindingVanillaKey = null;
			context.configManager().save(context);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private List<ClientTab> categories() {
		List<ClientTab> categories = new ArrayList<>();
		for (ClientTab tab : ServerTabManager.globalTabs(context)) {
			if (tab.all()) {
				continue;
			}
			boolean hasVisibleModules = visibleModules().stream().anyMatch(module -> moduleVisibleForCurrentSelection(module, tab));
			if (hasVisibleModules || tab.vip()) {
				categories.add(tab);
			}
		}
		if (categories.stream().noneMatch(tab -> tab.id().equals(selectedCategory))) {
			selectedCategory = categories.isEmpty() ? ServerTabManager.globalTabId("Other") : categories.get(0).id();
		}
		return categories;
	}

	private ClientTab selectedTab() {
		return categories().stream()
			.filter(tab -> tab.id().equals(selectedCategory))
			.findFirst()
			.orElse(categories().isEmpty() ? ServerTabManager.globalTab("Other") : categories().get(0));
	}

	private void selectTab(ClientTab tab) {
		if (tab == null) {
			selectedCategory = ServerTabManager.ALL_TAB_ID;
			return;
		}
		if (tab.vip() && !VipAccessManager.isUnlocked()) {
			if (client != null) {
				client.setScreen(new VipPasswordScreen(this, unlocked -> {
					if (unlocked) {
						selectedCategory = tab.id();
					}
				}));
			}
			return;
		}
		selectedCategory = tab.id();
		tabPickerModule = null;
	}

	private void openTabPicker(Module module, int localX, int localY) {
		tabPickerModule = module;
		tabPickerX = localX;
		tabPickerY = localY;
	}

	private boolean handleTabPickerClick(double mouseX, double mouseY) {
		if (tabPickerModule == null) {
			return false;
		}

		List<ClientTab> tabs = assignableTabs();
		int rowHeight = 18;
		int panelWidth = 188;
		int panelHeight = 8 + (tabs.size() * rowHeight);
		if (!UiRenderer.isHovered(mouseX, mouseY, sx(tabPickerX), sy(tabPickerY), ss(panelWidth), ss(panelHeight))) {
			tabPickerModule = null;
			return false;
		}

		for (int i = 0; i < tabs.size(); i++) {
			ClientTab tab = tabs.get(i);
			int rowY = tabPickerY + 4 + (i * rowHeight);
			if (!UiRenderer.isHovered(mouseX, mouseY, sx(tabPickerX + 4), sy(rowY), ss(panelWidth - 8), ss(rowHeight - 2))) {
				continue;
			}
			if (tab.vip() && !VipAccessManager.isUnlocked()) {
				if (client != null) {
					client.setScreen(new VipPasswordScreen(this, unlocked -> {
						if (unlocked) {
							applyTabAssignment(tabPickerModule, tab);
						}
					}));
				}
				tabPickerModule = null;
				return true;
			}
			applyTabAssignment(tabPickerModule, tab);
			tabPickerModule = null;
			return true;
		}

		return false;
	}

	private boolean handleServersClick(double mouseX, double mouseY, int x, int y, int width, int height) {
		if (UiRenderer.isHovered(mouseX, mouseY, sx(x + width - 154), sy(y + 68), ss(118), ss(20))) {
			String liveKey = ServerTabManager.currentServerKey(client);
			selectServerKey(liveKey);
			return true;
		}

		int rowY = y + 126;
		for (String serverKey : availableServerKeys()) {
			if (UiRenderer.isHovered(mouseX, mouseY, sx(x + 18), sy(rowY), ss(width - 36), ss(34))) {
				selectServerKey(serverKey);
				return true;
			}
			rowY += 42;
		}
		return false;
	}

	private List<ClientTab> assignableTabs() {
		List<ClientTab> tabs = new ArrayList<>(ServerTabManager.globalTabs(context).stream()
			.filter(ClientTab::isAssignable)
			.toList());
		if (!selectedServerKey.isBlank()) {
			String label = serverLabel(selectedServerKey);
			for (String category : context.moduleManager().getCategories()) {
				if ("VIP".equals(category)) {
					continue;
				}
				tabs.add(ServerTabManager.serverTab(category, selectedServerKey, label));
			}
		}
		return tabs;
	}

	private void applyTabAssignment(Module module, ClientTab tab) {
		if (module == null || tab == null) {
			return;
		}
		if (module.canRemoveFromTab(tab)) {
			module.removeFromTab(tab);
		} else {
			module.assignToTab(tab);
		}
		context.configManager().save(context);
	}

	private void renderTabPicker(DrawContext drawContext, int mouseX, int mouseY, int accentColor, int textColor) {
		if (tabPickerModule == null) {
			return;
		}
		List<ClientTab> tabs = assignableTabs();
		int rowHeight = 18;
		int panelWidth = 188;
		int panelHeight = 8 + (tabs.size() * rowHeight);
		UiRenderer.drawPanel(drawContext, sx(tabPickerX), sy(tabPickerY), ss(panelWidth), ss(panelHeight), 0xF01A2320, 0xFF344139);
		for (int i = 0; i < tabs.size(); i++) {
			ClientTab tab = tabs.get(i);
			int rowY = tabPickerY + 4 + (i * rowHeight);
			boolean hovered = UiRenderer.isHovered(mouseX, mouseY, sx(tabPickerX + 4), sy(rowY), ss(panelWidth - 8), ss(rowHeight - 2));
			int fill = hovered ? UiRenderer.withAlpha(accentColor, 84) : 0x00000000;
			if (fill != 0) {
				UiRenderer.drawPanel(drawContext, sx(tabPickerX + 4), sy(rowY), ss(panelWidth - 8), ss(rowHeight - 2), fill, 0x00000000);
			}
			String action;
			if (tab.baseCategory().equals(tabPickerModule.getOriginalCategory()) && tab.isGlobal()) {
				action = tabPickerModule.isOriginalCategoryHidden() ? "Restore" : "Hide";
			} else {
				action = tabPickerModule.canRemoveFromTab(tab) ? "Remove" : (tabPickerModule.belongsToTab(tab) ? "Base" : "Add");
			}
			UiRenderer.drawTrimmedText(drawContext, textRenderer, action, sx(tabPickerX + 10), sy(rowY + 5), ss(44), UiRenderer.withAlpha(textColor, 150));
			UiRenderer.drawTrimmedText(drawContext, textRenderer, tab.label(), sx(tabPickerX + 58), sy(rowY + 5), ss(panelWidth - 68), tab.vip() ? 0xFFF0C54A : textColor);
		}
	}

	private void copyModuleLayoutToClipboard() {
		StringBuilder builder = new StringBuilder();
		for (ClientTab category : categories()) {
			if (category.all()) {
				continue;
			}
			builder.append(category.label()).append(':').append('\n');
			context.moduleManager().getModules().stream()
				.filter(module -> !module.isHidden() && module.belongsToTab(category))
				.sorted(Comparator.comparing(Module::getName))
				.forEach(module -> builder.append("- ").append(module.getName()).append('\n'));
			builder.append('\n');
		}
		if (client != null) {
			client.keyboard.setClipboard(builder.toString().trim());
		}
	}

	private List<KeybindEntry> keybindEntries() {
		List<KeybindEntry> entries = new ArrayList<>();
		String query = searchField == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
		addKeybindEntry(entries, query, new KeybindEntry("Client", "Open Click GUI", displayKeyBinding(context.openGuiKey()), bindingGuiKey, KeybindType.OPEN_GUI, context.openGuiKey()));
		addKeybindEntry(entries, query, new KeybindEntry("Client", "Panic Stop All Mods", displayKeyBinding(context.panicKey()), bindingPanicKey, KeybindType.PANIC, context.panicKey()));

		if (client == null || client.options == null || client.options.allKeys == null) {
			return entries;
		}

		List<KeyBinding> bindings = new ArrayList<>(List.of(client.options.allKeys));
		bindings.sort(Comparator
			.comparing((KeyBinding binding) -> Text.translatable(binding.getCategory()).getString(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(binding -> Text.translatable(binding.getTranslationKey()).getString(), String.CASE_INSENSITIVE_ORDER));

		for (KeyBinding binding : bindings) {
			if (binding == context.openGuiKey() || binding == context.panicKey()) {
				continue;
			}

			String category = Text.translatable(binding.getCategory()).getString();
			String label = Text.translatable(binding.getTranslationKey()).getString();
			addKeybindEntry(entries, query, new KeybindEntry(category, label, displayKeyBinding(binding), binding == bindingVanillaKey, KeybindType.VANILLA, binding));
		}

		return entries;
	}

	private void addKeybindEntry(List<KeybindEntry> entries, String query, KeybindEntry entry) {
		if (query.isEmpty()) {
			entries.add(entry);
			return;
		}

		String haystack = (entry.category() + " " + entry.label() + " " + entry.keyName()).toLowerCase(Locale.ROOT);
		if (haystack.contains(query)) {
			entries.add(entry);
		}
	}

	private List<Module> filteredModules() {
		String query = searchField == null ? "" : searchField.getText().strip().toLowerCase(Locale.ROOT);
		ClientTab selectedTab = selectedTab();
		return visibleModules().stream()
			.filter(module -> moduleVisibleForCurrentSelection(module, selectedTab))
			.filter(module -> query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(query) || module.getDescription().toLowerCase(Locale.ROOT).contains(query))
			.toList();
	}

	private boolean moduleVisibleForCurrentSelection(Module module, ClientTab globalTab) {
		if (globalTab == null || globalTab.all()) {
			if (selectedServerKey.isBlank()) {
				return true;
			}
			for (String category : context.moduleManager().getCategories()) {
				ClientTab serverTab = ServerTabManager.serverTab(category, selectedServerKey, serverLabel(selectedServerKey));
				if (module.belongsToTab(serverTab)) {
					return true;
				}
			}
			if (!module.isOriginalCategoryHidden()) {
				return true;
			}
			return module.getExtraCategories().stream().anyMatch(extra -> extra != null && (extra.startsWith("global|") || extra.equals(module.getOriginalCategory())));
		}

		if (module.belongsToTab(globalTab)) {
			return true;
		}

		if (!selectedServerKey.isBlank()) {
			ClientTab serverTab = ServerTabManager.serverTab(globalTab.baseCategory(), selectedServerKey, serverLabel(selectedServerKey));
			return module.belongsToTab(serverTab);
		}
		return false;
	}

	private List<String> availableServerKeys() {
		List<String> keys = new ArrayList<>();
		keys.add("");
		for (String key : context.configManager().getConfig().knownServers.keySet()) {
			if (key != null && !key.isBlank() && !keys.contains(key)) {
				keys.add(key);
			}
		}
		String liveKey = ServerTabManager.currentServerKey(client);
		if (!liveKey.isBlank() && !keys.contains(liveKey)) {
			keys.add(liveKey);
		}
		return keys;
	}

	private void selectServerKey(String serverKey) {
		selectedServerKey = serverKey == null ? "" : serverKey;
		context.configManager().getConfig().activeServerKey = selectedServerKey;
		context.configManager().save(context);
		tabPickerModule = null;
	}

	private String currentServerSelectionLabel() {
		if (selectedServerKey.isBlank()) {
			return "Global / all servers";
		}
		return serverLabel(selectedServerKey);
	}

	private String serverLabel(String serverKey) {
		if (serverKey == null || serverKey.isBlank()) {
			return "Global";
		}
		String stored = context.configManager().getConfig().knownServers.get(serverKey);
		if (stored != null && !stored.isBlank()) {
			return stored;
		}
		return serverKey;
	}

	private List<Module> visibleModules() {
		return context.moduleManager().getModules().stream()
			.filter(module -> !module.isHidden())
			.toList();
	}

	private List<Module> hiddenModules() {
		return context.moduleManager().getModules().stream()
			.filter(Module::isHidden)
			.toList();
	}

	private List<HudElement> visibleHudElements() {
		return context.hudManager().getElements().stream()
			.filter(element -> !"real_balance".equals(element.getId()))
			.toList();
	}

	private int maxModuleScroll() {
		int rows = (int) Math.ceil(filteredModules().size() / 3.0);
		return Math.max(0, (rows * 138) - 386);
	}

	private int keybindContentHeight(List<KeybindEntry> entries) {
		int contentHeight = 0;
		String lastCategory = "";
		for (KeybindEntry entry : entries) {
			if (!entry.category().equals(lastCategory)) {
				contentHeight += 22;
				lastCategory = entry.category();
			}
			contentHeight += 42;
		}
		return contentHeight;
	}

	private int maxKeybindScroll() {
		return Math.max(0, keybindContentHeight(keybindEntries()) - (456 - 84));
	}

	private int maxHudScroll() {
		return Math.max(0, Math.max(332, visibleHudElements().size() * 38) - 332);
	}

	private int maxProfileScroll() {
		int viewportHeight = 278 - 12;
		return Math.max(0, Math.max(viewportHeight, context.profileManager().getProfiles().size() * 38) - viewportHeight);
	}

	private float uiScale() {
		return MathHelper.clamp(context.configManager().getConfig().theme.uiScale, 1.0F, 1.4F);
	}

	private int guiLeft() {
		return (width - Math.round(BASE_WIDTH * uiScale())) / 2;
	}

	private int guiTop() {
		return (height - Math.round(BASE_HEIGHT * uiScale())) / 2;
	}

	private int animatedTop() {
		return guiTop() + Math.round((float) ((1.0 - openProgress) * 22.0) * uiScale());
	}

	private int sx(int localX) {
		return guiLeft() + Math.round(localX * uiScale());
	}

	private int sy(int localY) {
		return animatedTop() + Math.round(localY * uiScale());
	}

	private int ss(int localSize) {
		return Math.max(1, Math.round(localSize * uiScale()));
	}

	private int sliderBarLocalX(int x) {
		return x + 180;
	}

	private int sliderBarLocalY(int y) {
		return y + 14;
	}

	private int sliderBarLocalWidth(int width) {
		return width - 260;
	}

	private int sliderBarLocalHeight() {
		return 6;
	}

	private int settingsViewportHeight(int height) {
		return height - 68;
	}

	private int maxSettingsScroll(int height) {
		return Math.max(0, settingsContentHeight() - settingsViewportHeight(height));
	}

	private int maxThemePresetScroll() {
		int previewViewportHeight = 500 - 190;
		int contentHeight = (ThemePresets.all().size() * 58) - 10;
		return Math.max(0, contentHeight - previewViewportHeight);
	}

	private String displayKeyBinding(KeyBinding keyBinding) {
		if (keyBinding == null || "key.keyboard.unknown".equals(keyBinding.getBoundKeyTranslationKey())) {
			return Module.UNBOUND_BIND_TEXT;
		}
		return keyBinding.getBoundKeyLocalizedText().getString();
	}

	private int settingsContentHeight() {
		if (showHiddenModulesSettings) {
			return Math.max(64, hiddenModules().size() * 58);
		}
		return 472;
	}

	private void renderScrollBar(DrawContext drawContext, int x, int y, int height, int scroll, int maxScroll, int accentColor) {
		if (maxScroll <= 0) {
			return;
		}

		int trackX = sx(x);
		int trackY = sy(y);
		int trackWidth = ss(4);
		int trackHeight = ss(height);
		UiRenderer.fillRoundedRect(drawContext, trackX, trackY, trackWidth, trackHeight, 2, 0x44212A28);

		int thumbHeight = Math.max(22, Math.round((height / (float) settingsContentHeight()) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((scroll / (float) maxScroll) * travel);
		UiRenderer.fillRoundedRect(drawContext, trackX, sy(thumbY), trackWidth, ss(thumbHeight), 2, UiRenderer.withAlpha(accentColor, 220));
	}

	private void renderPanelScrollBar(DrawContext drawContext, int x, int y, int height, int scroll, int maxScroll, int contentHeight, int accentColor) {
		if (maxScroll <= 0 || contentHeight <= 0) {
			return;
		}

		int trackX = sx(x);
		int trackY = sy(y);
		int trackWidth = ss(4);
		int trackHeight = ss(height);
		UiRenderer.fillRoundedRect(drawContext, trackX, trackY, trackWidth, trackHeight, 2, 0x44212A28);

		int thumbHeight = Math.max(22, Math.round((height / (float) contentHeight) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((scroll / (float) maxScroll) * travel);
		UiRenderer.fillRoundedRect(drawContext, trackX, sy(thumbY), trackWidth, ss(thumbHeight), 2, UiRenderer.withAlpha(accentColor, 220));
	}

	private boolean beginScrollDrag(double mouseX, double mouseY) {
		if (activePanel == ScreenPanel.MODULES && isScrollThumbHovered(mouseX, mouseY, 20 + 670 - 10, 54 + 44, 386, moduleScroll, maxModuleScroll(), Math.max(386, (int) Math.ceil(filteredModules().size() / 3.0) * 138))) {
			scrollDragTarget = ScrollDragTarget.MODULES;
			updateScrollDrag(mouseY);
			return true;
		}
		if (activePanel == ScreenPanel.KEYBINDS && isScrollThumbHovered(mouseX, mouseY, 20 + 670 - 10, 54 + 70, 456 - 84, keybindScroll, maxKeybindScroll(), keybindContentHeight(keybindEntries()))) {
			scrollDragTarget = ScrollDragTarget.KEYBINDS;
			updateScrollDrag(mouseY);
			return true;
		}
		if (activePanel == ScreenPanel.SETTINGS && isScrollThumbHovered(mouseX, mouseY, 20 + 670 - 10, 54 + 54, settingsViewportHeight(456), settingsScroll, maxSettingsScroll(456), settingsContentHeight())) {
			scrollDragTarget = ScrollDragTarget.SETTINGS;
			updateScrollDrag(mouseY);
			return true;
		}
		if (activePanel == ScreenPanel.SETTINGS && !showHiddenModulesSettings && isScrollThumbHovered(mouseX, mouseY, 724 + 178 + 4, 190, 500 - 190, themePresetScroll, maxThemePresetScroll(), (ThemePresets.all().size() * 58) - 10)) {
			scrollDragTarget = ScrollDragTarget.THEME_PRESETS;
			updateScrollDrag(mouseY);
			return true;
		}
		if (activePanel == ScreenPanel.HUD && isScrollThumbHovered(mouseX, mouseY, 20 + 670 - 10, 54 + 72, 456 - 124, hudScroll, maxHudScroll(), Math.max(332, visibleHudElements().size() * 38))) {
			scrollDragTarget = ScrollDragTarget.HUD;
			updateScrollDrag(mouseY);
			return true;
		}
		if (activePanel == ScreenPanel.PROFILES && isScrollThumbHovered(mouseX, mouseY, 20 + 670 - 10, 54 + 166, 278 - 12, profileScroll, maxProfileScroll(), Math.max(266, context.profileManager().getProfiles().size() * 38))) {
			scrollDragTarget = ScrollDragTarget.PROFILES;
			updateScrollDrag(mouseY);
			return true;
		}
		return false;
	}

	private void updateScrollDrag(double mouseY) {
		if (scrollDragTarget == null) {
			return;
		}

		switch (scrollDragTarget) {
			case MODULES -> moduleScroll = dragToScroll(mouseY, 54 + 44, 386, maxModuleScroll(), Math.max(386, (int) Math.ceil(filteredModules().size() / 3.0) * 138));
			case KEYBINDS -> keybindScroll = dragToScroll(mouseY, 54 + 70, 456 - 84, maxKeybindScroll(), keybindContentHeight(keybindEntries()));
			case SETTINGS -> settingsScroll = dragToScroll(mouseY, 54 + 54, settingsViewportHeight(456), maxSettingsScroll(456), settingsContentHeight());
			case THEME_PRESETS -> themePresetScroll = dragToScroll(mouseY, 190, 500 - 190, maxThemePresetScroll(), (ThemePresets.all().size() * 58) - 10);
			case HUD -> hudScroll = dragToScroll(mouseY, 54 + 72, 456 - 124, maxHudScroll(), Math.max(332, visibleHudElements().size() * 38));
			case PROFILES -> profileScroll = dragToScroll(mouseY, 54 + 166, 278 - 12, maxProfileScroll(), Math.max(266, context.profileManager().getProfiles().size() * 38));
		}
	}

	private int dragToScroll(double mouseY, int trackLocalY, int trackHeight, int maxScroll, int contentHeight) {
		if (maxScroll <= 0 || contentHeight <= 0) {
			return 0;
		}

		int thumbHeight = Math.max(22, Math.round((trackHeight / (float) contentHeight) * trackHeight));
		int travel = Math.max(1, trackHeight - thumbHeight);
		double localMouseY = (mouseY - sy(trackLocalY)) / uiScale();
		double clamped = MathHelper.clamp(localMouseY - (thumbHeight / 2.0), 0.0, travel);
		return Math.round((float) ((clamped / travel) * maxScroll));
	}

	private boolean isScrollThumbHovered(double mouseX, double mouseY, int x, int y, int height, int scroll, int maxScroll, int contentHeight) {
		if (maxScroll <= 0 || contentHeight <= 0) {
			return false;
		}

		int thumbHeight = Math.max(22, Math.round((height / (float) contentHeight) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((scroll / (float) maxScroll) * travel);
		return UiRenderer.isHovered(mouseX, mouseY, sx(x), sy(thumbY), ss(8), ss(thumbHeight));
	}

	private record KeybindEntry(String category, String label, String keyName, boolean binding, KeybindType type, KeyBinding keyBinding) {
	}

	private enum KeybindType {
		OPEN_GUI,
		PANIC,
		VANILLA
	}

	private enum ScrollDragTarget {
		MODULES,
		KEYBINDS,
		SETTINGS,
		THEME_PRESETS,
		HUD,
		PROFILES
	}

	private void renderFishBand(DrawContext drawContext, long now, int y, int width, int height, float alpha, boolean moveRight, double phase) {
		int gap = 84;
		int step = width + gap;
		int span = BASE_WIDTH + step;
		int travel = (int) Math.round((now / (moveRight ? 30.0 : 34.0)) + (phase * 113.0)) % span;
		int baseX = moveRight ? -width + travel : BASE_WIDTH - travel;

		for (int offset = -2; offset <= 4; offset++) {
			int drawX = baseX + (moveRight ? offset * step : -offset * step);
			int bob = (int) Math.round(Math.sin((now / 680.0) + phase + (offset * 0.65)) * 6.0);
			drawLocalTexture(drawContext, TROPICAL_FISH_TEXTURE, drawX, y + bob, width, height, alpha);
		}
	}

	private void renderWaterTextureOverlay(DrawContext drawContext, long now) {
		fillLocalRect(drawContext, 0, 0, BASE_WIDTH, BASE_HEIGHT, UiRenderer.withAlpha(0xFF1592AD, 16));
		for (int lane = 0; lane < 4; lane++) {
			int width = 304;
			int height = 104;
			int travel = (int) Math.round((now / (42.0 + (lane * 4.0))) + (lane * 71.0)) % (BASE_WIDTH + width + 120);
			int drawX = -width + travel;
			int drawY = 34 + (lane * 118) + (int) Math.round(Math.sin((now / 820.0) + lane) * 4.0);
			drawLocalTexture(drawContext, TROPICAL_WATER_TEXTURE, drawX, drawY, width, height, 0.18F);
			drawLocalTexture(drawContext, TROPICAL_WATER_TEXTURE, drawX - width - 72, drawY + 10, width, height, 0.12F);
		}
	}

	private void renderFallbackWaterOverlay(DrawContext drawContext, long now) {
		int waterTint = UiRenderer.withAlpha(0xFF1EA3C4, 18);
		int waterColor = UiRenderer.withAlpha(0xFF1EA3C4, 42);
		int foamColor = UiRenderer.withAlpha(0xFF9AF9FF, 74);
		fillLocalRect(drawContext, 0, 0, BASE_WIDTH, BASE_HEIGHT, waterTint);
		for (int row = 0; row < 16; row++) {
			int baseY = 60 + (row * 28);
			for (int col = 0; col < 10; col++) {
				int waveX = -20 + (col * 108) + (int) Math.round(Math.sin((now / 540.0) + (row * 0.7) + (col * 0.45)) * 12.0);
				int waveY = baseY + (int) Math.round(Math.cos((now / 710.0) + (col * 0.55)) * 4.0);
				fillLocalRect(drawContext, waveX, waveY, 88, 2, waterColor);
				fillLocalRect(drawContext, waveX + 12, waveY + 4, 48, 1, foamColor);
			}
		}
	}

	private boolean hasGuiTexture(Identifier texture) {
		return client != null && client.getResourceManager().getResource(texture).isPresent();
	}

	private void drawLocalTexture(DrawContext drawContext, Identifier texture, int localX, int localY, int localWidth, int localHeight, float alpha) {
		if (alpha <= 0.0F || localWidth <= 0 || localHeight <= 0) {
			return;
		}

		int screenWidth = ss(localWidth);
		int screenHeight = ss(localHeight);
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(alpha, 0.0F, 1.0F));
		drawContext.drawTexture(RenderLayer::getGuiTextured, texture, sx(localX), sy(localY), 0.0F, 0.0F, screenWidth, screenHeight, screenWidth, screenHeight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void fillLocalRect(DrawContext drawContext, int localX, int localY, int localWidth, int localHeight, int color) {
		drawContext.fill(sx(localX), sy(localY), sx(localX) + ss(localWidth), sy(localY) + ss(localHeight), color);
	}

	private void drawOverlayRectIfVisible(DrawContext drawContext, int localX, int localY, int width, int height, int color) {
		for (int x = 0; x < width; x += 6) {
			for (int y = 0; y < height; y += 6) {
				int segmentWidth = Math.min(6, width - x);
				int segmentHeight = Math.min(6, height - y);
				int segmentSize = Math.max(segmentWidth, segmentHeight);
				if (!isStarVisible(localX + x, localY + y, segmentSize)) {
					continue;
				}

				int screenX = sx(localX + x);
				int screenY = sy(localY + y);
				drawContext.fill(screenX, screenY, screenX + ss(segmentWidth), screenY + ss(segmentHeight), color);
			}
		}
	}

	private void drawPalmFrond(DrawContext drawContext, int anchorX, int anchorY, boolean faceRight, int leafColor, int highlightColor) {
		int direction = faceRight ? 1 : -1;
		for (int index = 0; index < 5; index++) {
			int leafWidth = 34 - (index * 4);
			int leafX = anchorX + (direction * index * 14);
			int leafY = anchorY + (index * 11);
			int rectX = faceRight ? leafX : leafX - leafWidth;
			drawOverlayRectIfVisible(drawContext, rectX, leafY, leafWidth, 4, leafColor);
			drawOverlayRectIfVisible(drawContext, rectX + (faceRight ? 4 : 0), leafY + 4, Math.max(10, leafWidth - 10), 2, highlightColor);
		}
	}

	private boolean isStarVisible(int localX, int localY, int size) {
		if (intersects(localX, localY, size, 12, 6, BASE_WIDTH - 24, 42)) {
			return false;
		}
		if (intersectsPreviewText(localX, localY, size)) {
			return false;
		}
		return switch (activePanel) {
			case MODULES -> !intersectsModuleText(localX, localY, size);
			case KEYBINDS -> !intersectsKeybindText(localX, localY, size);
			case SETTINGS -> !intersectsSettingsText(localX, localY, size);
			case HUD -> !intersectsHudText(localX, localY, size);
			case PROFILES -> !intersectsProfileText(localX, localY, size);
			case SERVERS -> true;
		};
	}

	private boolean intersectsModuleText(int localX, int localY, int size) {
		if (intersects(localX, localY, size, 20, 54, 670, 28) || intersects(localX, localY, size, 718, 79, 156, 12)) {
			return true;
		}

		int contentX = 36;
		int contentY = 100 - moduleScroll;
		int cardWidth = 202;
		int cardHeight = 126;
		int gap = 12;
		List<Module> modules = filteredModules();
		for (int index = 0; index < modules.size(); index++) {
			int column = index % 3;
			int row = index / 3;
			int cardX = contentX + (column * (cardWidth + gap));
			int cardY = contentY + (row * (cardHeight + gap));
			if (intersects(localX, localY, size, cardX + 16, cardY + 19, cardWidth - 32, 12)
				|| intersects(localX, localY, size, cardX + 16, cardY + 37, cardWidth - 32, 34)
				|| intersects(localX, localY, size, cardX + 22, cardY + 87, cardWidth - 44, 10)
				|| intersects(localX, localY, size, cardX + 71, cardY + 106, 60, 10)) {
				return true;
			}
		}
		return false;
	}

	private boolean intersectsKeybindText(int localX, int localY, int size) {
		return intersects(localX, localY, size, 20, 54, 670, 456);
	}

	private boolean intersectsSettingsText(int localX, int localY, int size) {
		if (showHiddenModulesSettings) {
			return intersects(localX, localY, size, 20, 54, 670, 456);
		}
		if (intersects(localX, localY, size, 20, 54, 670, 52)) {
			return true;
		}

		int rowY = 108 - settingsScroll;
		for (int i = 0; i < 3; i++) {
			if (intersects(localX, localY, size, 50, rowY + 8, 140, 12)
				|| intersects(localX, localY, size, 594, rowY + 8, 48, 12)) {
				return true;
			}
			rowY += 42;
		}
		for (int i = 0; i < 3; i++) {
			if (intersects(localX, localY, size, 50, rowY + 8, 120, 12)) {
				return true;
			}
			rowY += 34;
		}
		rowY += 8;
		for (int i = 0; i < 3; i++) {
			if (intersects(localX, localY, size, 50, rowY + 8, 80, 12)
				|| intersects(localX, localY, size, 594, rowY + 8, 48, 12)) {
				return true;
			}
			rowY += 42;
		}

		if (intersects(localX, localY, size, 50, rowY + 12, 140, 12)
			|| intersects(localX, localY, size, 280, rowY + 13, 56, 10)
			|| intersects(localX, localY, size, 367, rowY + 12, 120, 12)
			|| intersects(localX, localY, size, 598, rowY + 13, 56, 10)) {
			return true;
		}

		rowY += 42;
		return intersects(localX, localY, size, 50, rowY + 12, 140, 12)
			|| intersects(localX, localY, size, 598, rowY + 13, 56, 10);
	}

	private boolean intersectsPreviewText(int localX, int localY, int size) {
		if (activePanel == ScreenPanel.SETTINGS) {
			if (intersects(localX, localY, size, 724, 124, 178, 16) || intersects(localX, localY, size, 724, 142, 178, 44)) {
				return true;
			}

			int cardY = 192 - themePresetScroll;
			for (ThemePreset ignored : ThemePresets.all()) {
				if (intersects(localX, localY, size, 802, cardY + 12, 88, 20)) {
					return true;
				}
				cardY += 58;
			}
			return false;
		}

		if (selectedModule == null) {
			return intersects(localX, localY, size, 724, 126, 120, 12);
		}

		return intersects(localX, localY, size, 724, 124, 178, 16)
			|| intersects(localX, localY, size, 724, 142, 178, 48)
			|| intersects(localX, localY, size, 724, 206, 70, 12)
			|| intersects(localX, localY, size, 736, 224, 72, 10)
			|| intersects(localX, localY, size, 724, 252, 50, 12)
			|| intersects(localX, localY, size, 746, 270, 52, 10)
			|| intersects(localX, localY, size, 724, 298, 52, 12)
			|| intersects(localX, localY, size, 736, 316, 108, 10)
			|| intersects(localX, localY, size, 724, 352, 30, 12)
			|| intersects(localX, localY, size, 724, 366, 176, 62)
			|| intersects(localX, localY, size, 772, 461, 100, 10);
	}

	private boolean intersectsHudText(int localX, int localY, int size) {
		if (intersects(localX, localY, size, 20, 54, 670, 98)) {
			return true;
		}

		int rowY = 126;
		for (HudElement ignored : visibleHudElements()) {
			if (intersects(localX, localY, size, 38, rowY, 634, 30)) {
				return true;
			}
			rowY += 38;
		}
		return intersects(localX, localY, size, 38, 470, 148, 22);
	}

	private boolean intersectsProfileText(int localX, int localY, int size) {
		if (intersects(localX, localY, size, 20, 54, 670, 98)
			|| intersects(localX, localY, size, 38, 102, 620, 52)
			|| intersects(localX, localY, size, 38, 170, 634, 34)) {
			return true;
		}

		int rowY = 226;
		for (ProfileEntry ignored : context.profileManager().getProfiles()) {
			if (intersects(localX, localY, size, 50, rowY, 610, 30)) {
				return true;
			}
			rowY += 38;
		}
		return false;
	}

	private boolean intersects(int localX, int localY, int size, int rectX, int rectY, int rectWidth, int rectHeight) {
		int starRight = localX + size;
		int starBottom = localY + size;
		int rectRight = rectX + rectWidth;
		int rectBottom = rectY + rectHeight;
		return localX < rectRight && starRight > rectX && localY < rectBottom && starBottom > rectY;
	}

	private int getThemeColor(String target) {
		return switch (target) {
			case "background" -> context.configManager().getConfig().theme.backgroundColor;
			case "text" -> context.configManager().getConfig().theme.textColor;
			case "accent" -> context.configManager().getConfig().theme.accentColor;
			default -> context.configManager().getConfig().theme.accentColor;
		};
	}

	private void setThemeColor(String target, int color) {
		switch (target) {
			case "background" -> context.configManager().getConfig().theme.backgroundColor = color;
			case "text" -> context.configManager().getConfig().theme.textColor = color;
			case "accent" -> context.configManager().getConfig().theme.accentColor = color;
			default -> {
			}
		}
	}

	private void setThemeColorChannel(String target, int shift, int channelValue) {
		int color = getThemeColor(target);
		int rgb = color & 0x00FFFFFF;
		rgb = (rgb & ~(0xFF << shift)) | ((channelValue & 0xFF) << shift);
		setThemeColor(target, 0xFF000000 | rgb);
		context.configManager().save(context);
	}

	private void setModuleHidden(Module module, boolean hidden) {
		module.setHidden(hidden);
		if (hidden && module == selectedModule) {
			List<Module> visibleModules = visibleModules();
			selectedModule = visibleModules.isEmpty() ? null : visibleModules.get(0);
		} else if (!hidden) {
			selectedModule = module;
		}
		ClientTab selectedTab = selectedTab();
		if (!selectedTab.all() && visibleModules().stream().noneMatch(visible -> visible.belongsToTab(selectedTab))) {
			selectedCategory = ServerTabManager.ALL_TAB_ID;
		}
		moduleScroll = MathHelper.clamp(moduleScroll, 0, maxModuleScroll());
		settingsScroll = MathHelper.clamp(settingsScroll, 0, maxSettingsScroll(456));
		context.configManager().save(context);
	}

	private float red(int color) {
		return (color >> 16) & 0xFF;
	}

	private float green(int color) {
		return (color >> 8) & 0xFF;
	}

	private float blue(int color) {
		return color & 0xFF;
	}

	@FunctionalInterface
	private interface SliderConsumer {
		void accept(float value);
	}
}
