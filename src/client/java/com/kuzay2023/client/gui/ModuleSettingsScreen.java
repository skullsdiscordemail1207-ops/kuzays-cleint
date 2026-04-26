package com.kuzay2023.client.gui;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.gamble.GambleListerService;
import com.kuzay2023.client.gui.render.UiRenderer;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardClientHooks;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardConfig;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardScreen;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.AutoCraterSlotsModule;
import com.kuzay2023.client.module.impl.SetHomeHiddenModule;
import com.kuzay2023.client.module.impl.macro.KeyboardMacrosModule;
import com.kuzay2023.client.module.impl.gamble.GambleHelperModule;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.Setting;
import com.kuzay2023.client.module.impl.gamble.RigModModule;
import com.kuzay2023.client.module.setting.StringSetting;
import com.kuzay2023.client.module.impl.money.MoneyModuleUtil;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class ModuleSettingsScreen extends Screen {
	private static final int GUI_WIDTH = 620;
	private static final int GUI_HEIGHT = 430;
	private static final int CRAFTER_GRID_ROW_HEIGHT = 112;
	private static final int CRAFTER_GRID_PANEL_HEIGHT = 104;
	private static final int CRAFTER_GRID_CELL_SIZE = 18;
	private static final int CRAFTER_GRID_CELL_GAP = 4;

	private final KuzayClientContext context;
	private final Screen parent;
	private final Module module;

	private final Map<NumberSetting, TextFieldWidget> numberFields = new LinkedHashMap<>();
	private final Map<StringSetting, TextFieldWidget> stringFields = new LinkedHashMap<>();

	private NumberSetting draggedNumberSetting;
	private boolean bindingKey;
	private boolean bindingRigSwitchKey;
	private boolean bindingRigHudToggleKey;
	private boolean bindingGambleRemoveTopKey;
	private boolean bindingClickPayKey;
	private boolean bindingSetHomeKey;
	private int bindingKeyboardMacroIndex = -1;
	private int contentScroll;
	private boolean draggingScrollBar;

	public ModuleSettingsScreen(KuzayClientContext context, Screen parent, Module module) {
		super(Text.literal(module.getName()));
		this.context = context;
		this.parent = parent;
		this.module = module;
	}

	@Override
	protected void init() {
		clearChildren();
		numberFields.clear();
		stringFields.clear();

		int rowY = top() + 112;
		for (Setting<?> setting : module.getSettings()) {
			if (shouldHideSetting(setting)) {
				continue;
			}
			if (setting instanceof NumberSetting numberSetting) {
				TextFieldWidget field = new TextFieldWidget(textRenderer, left() + 472, rowY + 15, 90, 18, Text.literal(numberSetting.getName()));
				field.setMaxLength(32);
				field.setText(formatNumber(numberSetting));
				addDrawableChild(field);
				numberFields.put(numberSetting, field);
				rowY += 46;
			} else if (setting instanceof StringSetting stringSetting && usesCrafterGridSetting(stringSetting)) {
				rowY += CRAFTER_GRID_ROW_HEIGHT;
			} else if (setting instanceof StringSetting stringSetting && !usesItemPicker(stringSetting) && !usesCustomBindRow(stringSetting) && !isKeyboardMacroBindSetting(stringSetting)) {
				int fieldX = isMacroTextSetting(stringSetting) ? left() + 248 : left() + 280;
				int fieldWidth = isMacroTextSetting(stringSetting) ? 314 : 282;
				TextFieldWidget field = new TextFieldWidget(textRenderer, fieldX, rowY + 14, fieldWidth, 18, Text.literal(stringSetting.getName()));
				field.setMaxLength(128);
				field.setText(stringSetting.getValue());
				addDrawableChild(field);
				stringFields.put(stringSetting, field);
				rowY += 42;
			} else {
				rowY += 42;
			}
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
		UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingKey ? "Press a key..." : module.getBoundKeyName(), left() + 266, top() + 72, 102, textColor);

		UiRenderer.drawPanel(drawContext, left() + 394, top() + 60, 206, 34, mutedColor, 0xFF314037);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Click the state pill to toggle. Right here is where edits live.", left() + 406, top() + 65, 182, UiRenderer.withAlpha(textColor, 165));

		contentScroll = MathHelper.clamp(contentScroll, 0, maxContentScroll());
		layoutWidgets();
		drawContext.enableScissor(left() + 20, contentViewportTop(), left() + 600, contentViewportTop() + contentViewportHeight());

		int rowY = contentViewportTop() + 6 - contentScroll;
		if (module instanceof KeyboardMacrosModule keyboardMacrosModule) {
			UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, 34, mutedColor, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Macro Slots", left() + 32, rowY + 8, 220, textColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, Integer.toString(keyboardMacrosModule.getActiveMacroCount()), left() + 320, rowY + 8, 40, UiRenderer.withAlpha(textColor, 170));
			UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, keyboardMacrosModule.canAddMacro() ? accentColor : 0xFF24312A);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, keyboardMacrosModule.canAddMacro() ? "Add Macro" : "All Slots Used", left() + 448, rowY + 13, 92, keyboardMacrosModule.canAddMacro() ? 0xFF07131A : textColor);
			rowY += 42;
		}
		if (module instanceof RigModModule rigModModule) {
			UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, 34, mutedColor, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Switch Rig Side", left() + 32, rowY + 8, 220, textColor);
			UiRenderer.drawPill(drawContext, left() + 302, rowY + 7, 114, 20, accentColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Winner: " + rigModModule.getWinnerSideLabel(), left() + 316, rowY + 13, 86, 0xFF07131A);
			UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, bindingRigSwitchKey ? accentColor : 0xFF24312A);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingRigSwitchKey ? "Press a key..." : bindButtonText(displayKeyBinding(context.rigSwitchSidesKey())), left() + 428, rowY + 13, 124, bindingRigSwitchKey ? 0xFF07131A : textColor);
			rowY += 42;
		}
		if ("gambles_lister".equals(module.getId())) {
			UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, 34, mutedColor, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Clear Top Payment", left() + 32, rowY + 8, 220, textColor);
			UiRenderer.drawPill(drawContext, left() + 302, rowY + 7, 114, 20, accentColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Clear Top", left() + 337, rowY + 13, 44, 0xFF07131A);
			String keyName = displayKeyBinding(GambleListerService.getRemoveTopKey());
			UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, bindingGambleRemoveTopKey ? accentColor : 0xFF24312A);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingGambleRemoveTopKey ? "Press a key..." : bindButtonText(keyName), left() + 428, rowY + 13, 124, bindingGambleRemoveTopKey ? 0xFF07131A : textColor);
			rowY += 42;
		}
		if ("fake_scoreboard".equals(module.getId())) {
			FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
			UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, 34, mutedColor, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Fake Money Value", left() + 32, rowY + 8, 240, textColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, config == null ? "Unavailable" : FakeScoreboardConfig.formatMoney(config.getCurrentMoney()), left() + 280, rowY + 8, 120, UiRenderer.withAlpha(textColor, 170));
			UiRenderer.drawPill(drawContext, left() + 432, rowY + 7, 132, 20, accentColor);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Edit Money", left() + 470, rowY + 13, 56, 0xFF07131A);
			rowY += 42;
		}

		for (Setting<?> setting : module.getSettings()) {
			if (shouldHideSetting(setting)) {
				continue;
			}
			if (setting instanceof StringSetting stringSetting && usesCrafterGridSetting(stringSetting) && module instanceof AutoCraterSlotsModule autoCraterSlotsModule) {
				renderCrafterGridSetting(drawContext, rowY, stringSetting, autoCraterSlotsModule, textColor, mutedColor);
				rowY += CRAFTER_GRID_ROW_HEIGHT;
				continue;
			}

			if (setting instanceof StringSetting stringSetting && usesCustomBindRow(stringSetting)) {
				UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, 34, mutedColor, 0xFF313B34);
				if (isLookPayKeySetting(stringSetting) && module instanceof GambleHelperModule gambleHelperModule) {
					UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Look Pay Key", left() + 32, rowY + 8, 224, textColor);
					UiRenderer.drawPill(drawContext, left() + 302, rowY + 7, 114, 20, accentColor);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, "Open /pay", left() + 330, rowY + 13, 58, 0xFF07131A);
					UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, bindingClickPayKey ? accentColor : 0xFF24312A);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingClickPayKey ? "Press a key..." : bindButtonText(gambleHelperModule.getLookPayKeyName()), left() + 428, rowY + 13, 124, bindingClickPayKey ? 0xFF07131A : textColor);
				} else if (isSetHomeKeySetting(stringSetting) && module instanceof SetHomeHiddenModule setHomeHiddenModule) {
					UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Set Home Key", left() + 32, rowY + 8, 224, textColor);
					UiRenderer.drawPill(drawContext, left() + 302, rowY + 7, 114, 20, accentColor);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, "/sethome", left() + 334, rowY + 13, 50, 0xFF07131A);
					UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, bindingSetHomeKey ? accentColor : 0xFF24312A);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingSetHomeKey ? "Press a key..." : bindButtonText(setHomeHiddenModule.getSetHomeKeyName()), left() + 428, rowY + 13, 124, bindingSetHomeKey ? 0xFF07131A : textColor);
				} else if (isRigHudToggleKeySetting(stringSetting) && module instanceof RigModModule rigModModule) {
					UiRenderer.drawTrimmedText(drawContext, textRenderer, "General / Rig HUD Toggle Key", left() + 32, rowY + 8, 224, textColor);
					UiRenderer.drawPill(drawContext, left() + 302, rowY + 7, 114, 20, rigModModule.isHudVisibleSetting() ? 0xFF2EC27E : 0xFFAA2E57);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, rigModModule.isHudVisibleSetting() ? "Shown" : "Hidden", left() + 337, rowY + 13, 44, 0xFFF7F8F9);
					UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, bindingRigHudToggleKey ? accentColor : 0xFF24312A);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingRigHudToggleKey ? "Press a key..." : bindButtonText(rigModModule.getHudToggleKeyName()), left() + 428, rowY + 13, 124, bindingRigHudToggleKey ? 0xFF07131A : textColor);
				}
				rowY += 42;
				continue;
			}

			if (setting instanceof StringSetting stringSetting && isKeyboardMacroBindSetting(stringSetting) && module instanceof KeyboardMacrosModule keyboardMacrosModule) {
				int macroIndex = macroIndexFromSetting(stringSetting);
				UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, 34, mutedColor, 0xFF313B34);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, "Macro " + (macroIndex + 1) + " / Keybind", left() + 32, rowY + 8, 224, textColor);
				UiRenderer.drawPill(drawContext, left() + 416, rowY + 7, 148, 20, bindingKeyboardMacroIndex == macroIndex ? accentColor : 0xFF24312A);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, bindingKeyboardMacroIndex == macroIndex ? "Press a key..." : bindButtonText(keyboardMacrosModule.getMacroKeyName(macroIndex)), left() + 428, rowY + 13, 124, bindingKeyboardMacroIndex == macroIndex ? 0xFF07131A : textColor);
				rowY += 42;
				continue;
			}

			UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, setting instanceof NumberSetting ? 38 : 34, mutedColor, 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, setting.getSection() + " / " + setting.getName(), left() + 32, rowY + 8, 224, textColor);

			if (setting instanceof BooleanSetting booleanSetting) {
				UiRenderer.drawPill(drawContext, left() + 474, rowY + 7, 90, 20, booleanSetting.getValue() ? 0xFF2EC27E : 0xFF5D6268);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, booleanSetting.getValue() ? "ON" : "OFF", left() + 509, rowY + 13, 20, 0xFFF7F8F9);
				rowY += 42;
			} else if (setting instanceof EnumSetting enumSetting) {
				UiRenderer.drawPill(drawContext, left() + 386, rowY + 7, 178, 20, 0xFF2A3530);
				UiRenderer.drawTrimmedText(drawContext, textRenderer, enumSetting.getValue(), left() + 400, rowY + 13, 150, textColor);
				rowY += 42;
			} else if (setting instanceof NumberSetting numberSetting) {
				renderNumberSetting(drawContext, rowY, numberSetting, textColor, accentColor);
				rowY += 46;
			} else if (setting instanceof StringSetting stringSetting) {
				if (usesItemPicker(stringSetting)) {
					String label = displayItemLabel(stringSetting.getValue());
					UiRenderer.drawPill(drawContext, left() + 280, rowY + 7, 284, 20, 0xFF24312A);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, label, left() + 292, rowY + 13, 238, textColor);
					UiRenderer.drawTrimmedText(drawContext, textRenderer, "Pick", left() + 536, rowY + 13, 24, UiRenderer.withAlpha(textColor, 160));
				}
				rowY += 42;
			}
		}

		syncWidgetsFromSettings();
		super.render(drawContext, mouseX, mouseY, delta);
		drawContext.disableScissor();
		renderScrollBar(drawContext, left() + 604, contentViewportTop(), contentViewportHeight(), contentScroll, maxContentScroll(), accentColor);
	}

	private void renderNumberSetting(DrawContext drawContext, int rowY, NumberSetting setting, int textColor, int accentColor) {
		int barX = left() + 280;
		int barY = rowY + 21;
		int barWidth = 180;
		drawContext.fill(barX, barY, barX + barWidth, barY + 4, 0xFF202726);
		double range = setting.getMax() - setting.getMin();
		double percent = range <= 0.0 ? 0.0 : (setting.getValue() - setting.getMin()) / range;
		int fillWidth = (int) Math.round(barWidth * MathHelper.clamp(percent, 0.0, 1.0));
		UiRenderer.fillRoundedRect(drawContext, barX, barY, Math.max(4, fillWidth), 4, 2, accentColor);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, formatNumber(setting), left() + 420, rowY + 8, 38, UiRenderer.withAlpha(textColor, 170));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isScrollThumbHovered(mouseX, mouseY)) {
			draggingScrollBar = true;
			updateScrollFromMouse(mouseY);
			return true;
		}
		if (bindingKey) {
			module.setBoundKey(Module.encodeMouseButton(button));
			context.configManager().save(context);
			bindingKey = false;
			return true;
		}
		if (bindingKeyboardMacroIndex >= 0) {
			if (module instanceof KeyboardMacrosModule keyboardMacrosModule) {
				keyboardMacrosModule.setMacroMouseButton(bindingKeyboardMacroIndex, button);
			}
			context.configManager().save(context);
			bindingKeyboardMacroIndex = -1;
			return true;
		}
		commitTextFields();
		layoutWidgets();

		if (UiRenderer.isHovered(mouseX, mouseY, left() + 104, top() + 67, 72, 20)) {
			module.toggle();
			context.configManager().save(context);
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left() + 202, top() + 60, 180, 34)) {
			clearBindingStates();
			bindingKey = true;
			return true;
		}

		int rowY = contentViewportTop() + 6 - contentScroll;
		if (module instanceof KeyboardMacrosModule keyboardMacrosModule) {
			if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20) && keyboardMacrosModule.canAddMacro()) {
				keyboardMacrosModule.addMacro();
				context.configManager().save(context);
				init();
				return true;
			}
			rowY += 42;
		}
		if (module instanceof RigModModule rigModModule) {
			if (UiRenderer.isHovered(mouseX, mouseY, left() + 302, rowY + 7, 114, 20)) {
				rigModModule.switchSides();
				context.configManager().save(context);
				return true;
			}
			if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20)) {
				clearBindingStates();
				bindingRigSwitchKey = true;
				return true;
			}
			rowY += 42;
		}
		if ("gambles_lister".equals(module.getId())) {
			if (UiRenderer.isHovered(mouseX, mouseY, left() + 302, rowY + 7, 114, 20)) {
				GambleListerService.removeTopEntry();
				return true;
			}
			if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20)) {
				clearBindingStates();
				bindingGambleRemoveTopKey = true;
				return true;
			}
			rowY += 42;
		}
		if ("fake_scoreboard".equals(module.getId())) {
			if (UiRenderer.isHovered(mouseX, mouseY, left() + 432, rowY + 7, 132, 20) && client != null) {
				client.setScreen(new FakeScoreboardScreen(this));
				return true;
			}
			rowY += 42;
		}

		for (Setting<?> setting : module.getSettings()) {
			if (shouldHideSetting(setting)) {
				continue;
			}
			if (setting instanceof StringSetting stringSetting && usesCrafterGridSetting(stringSetting) && module instanceof AutoCraterSlotsModule autoCraterSlotsModule) {
				if (handleCrafterGridClick(mouseX, mouseY, rowY, autoCraterSlotsModule)) {
					context.configManager().save(context);
					return true;
				}
				rowY += CRAFTER_GRID_ROW_HEIGHT;
				continue;
			}

			if (setting instanceof BooleanSetting booleanSetting) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 474, rowY + 7, 90, 20)) {
					booleanSetting.setValue(!booleanSetting.getValue());
					context.configManager().save(context);
					return true;
				}
				rowY += 42;
			} else if (setting instanceof EnumSetting enumSetting) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 386, rowY + 7, 178, 20)) {
					enumSetting.cycle();
					context.configManager().save(context);
					return true;
				}
				rowY += 42;
			} else if (setting instanceof NumberSetting numberSetting) {
				if (UiRenderer.isHovered(mouseX, mouseY, left() + 280, rowY + 14, 180, 12)) {
					updateNumberSetting(numberSetting, mouseX);
					draggedNumberSetting = numberSetting;
					context.configManager().save(context);
					return true;
				}
				rowY += 46;
			} else if (setting instanceof StringSetting stringSetting) {
				if (isKeyboardMacroBindSetting(stringSetting) && module instanceof KeyboardMacrosModule) {
					if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20)) {
						clearBindingStates();
						bindingKeyboardMacroIndex = macroIndexFromSetting(stringSetting);
						return true;
					}
					rowY += 42;
					continue;
				}
				if (usesCustomBindRow(stringSetting)) {
					if (isLookPayKeySetting(stringSetting) && module instanceof GambleHelperModule) {
						if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20)) {
							clearBindingStates();
							bindingClickPayKey = true;
							return true;
						}
					} else if (isSetHomeKeySetting(stringSetting) && module instanceof SetHomeHiddenModule) {
						if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20)) {
							clearBindingStates();
							bindingSetHomeKey = true;
							return true;
						}
					} else if (isRigHudToggleKeySetting(stringSetting) && module instanceof RigModModule rigModModule) {
						if (UiRenderer.isHovered(mouseX, mouseY, left() + 302, rowY + 7, 114, 20)) {
							rigModModule.toggleHudVisibility();
							return true;
						}
						if (UiRenderer.isHovered(mouseX, mouseY, left() + 416, rowY + 7, 148, 20)) {
							clearBindingStates();
							bindingRigHudToggleKey = true;
							return true;
						}
					}
					rowY += 42;
					continue;
				}
				if (usesItemPicker(stringSetting) && UiRenderer.isHovered(mouseX, mouseY, left() + 280, rowY + 7, 284, 20)) {
					openItemPicker(stringSetting);
					return true;
				}
				rowY += 42;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (button == 0 && draggingScrollBar) {
			updateScrollFromMouse(mouseY);
			return true;
		}
		if (button == 0 && draggedNumberSetting != null) {
			updateNumberSetting(draggedNumberSetting, mouseX);
			context.configManager().save(context);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggedNumberSetting = null;
		draggingScrollBar = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (UiRenderer.isHovered(mouseX, mouseY, left() + 20, contentViewportTop(), 580, contentViewportHeight())) {
			contentScroll = MathHelper.clamp(contentScroll - (int) (verticalAmount * 24.0), 0, maxContentScroll());
			layoutWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingKeyboardMacroIndex >= 0) {
			if (module instanceof KeyboardMacrosModule keyboardMacrosModule) {
				keyboardMacrosModule.setMacroKey(bindingKeyboardMacroIndex, keyCode, scanCode);
			}
			context.configManager().save(context);
			bindingKeyboardMacroIndex = -1;
			return true;
		}
		if (bindingGambleRemoveTopKey) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				GambleListerService.stopListeningForRemoveKey();
			} else {
				GambleListerService.setRemoveTopKey(keyCode, scanCode);
				GambleListerService.stopListeningForRemoveKey();
			}
			context.configManager().save(context);
			bindingGambleRemoveTopKey = false;
			return true;
		}
		if (bindingClickPayKey) {
			if (module instanceof GambleHelperModule gambleHelperModule) {
				gambleHelperModule.setLookPayKey(keyCode, scanCode);
			}
			context.configManager().save(context);
			bindingClickPayKey = false;
			return true;
		}
		if (bindingSetHomeKey) {
			if (module instanceof SetHomeHiddenModule setHomeHiddenModule) {
				setHomeHiddenModule.setSetHomeKey(keyCode, scanCode);
			}
			context.configManager().save(context);
			bindingSetHomeKey = false;
			return true;
		}
		if (bindingRigSwitchKey) {
			context.rigSwitchSidesKey().setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
			KeyBinding.updateKeysByCode();
			context.configManager().save(context);
			bindingRigSwitchKey = false;
			if (client != null) {
				client.options.write();
			}
			return true;
		}
		if (bindingRigHudToggleKey) {
			if (module instanceof RigModModule rigModModule) {
				rigModModule.setHudToggleKey(keyCode, scanCode);
			}
			context.configManager().save(context);
			bindingRigHudToggleKey = false;
			return true;
		}
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

	private void openItemPicker(StringSetting setting) {
		if (client == null) {
			return;
		}
		if (usesBlockPicker(setting)) {
			client.setScreen(new BlockPickerScreen(this, blockId -> {
				setting.setValue(blockId);
				context.configManager().save(context);
			}));
			return;
		}
		client.setScreen(new ItemPickerScreen(this, itemId -> {
			applyItemSelection(setting, itemId);
			context.configManager().save(context);
		}));
	}

	private void applyItemSelection(StringSetting targetSetting, String itemId) {
		Item item = MoneyModuleUtil.resolveItem(itemId);
		String itemName = item == net.minecraft.item.Items.AIR ? itemId : item.getName().getString();
		targetSetting.setValue(itemId);
		for (Setting<?> sibling : module.getSettings()) {
			if (sibling instanceof StringSetting stringSetting && sibling != targetSetting) {
				if ("target_item_id".equals(stringSetting.getId())) {
					stringSetting.setValue(itemId);
				} else if ("item_name".equals(stringSetting.getId())) {
					stringSetting.setValue(itemName.toLowerCase(Locale.ROOT));
				}
			}
		}
	}

	private void updateNumberSetting(NumberSetting setting, double mouseX) {
		double percent = MathHelper.clamp((mouseX - (left() + 280)) / 180.0, 0.0, 1.0);
		double value = setting.getMin() + ((setting.getMax() - setting.getMin()) * percent);
		setting.setValue(setting.clamp(value));
		TextFieldWidget field = numberFields.get(setting);
		if (field != null && !field.isFocused()) {
			field.setText(formatNumber(setting));
		}
	}

	private void commitTextFields() {
		for (Map.Entry<NumberSetting, TextFieldWidget> entry : numberFields.entrySet()) {
			TextFieldWidget field = entry.getValue();
			if (!field.isFocused()) {
				try {
					double parsed = Double.parseDouble(field.getText().trim());
					entry.getKey().setValue(entry.getKey().clamp(parsed));
				} catch (NumberFormatException ignored) {
				}
				field.setText(formatNumber(entry.getKey()));
			}
		}

		for (Map.Entry<StringSetting, TextFieldWidget> entry : stringFields.entrySet()) {
			TextFieldWidget field = entry.getValue();
			entry.getKey().setValue(field.getText());
		}
		context.configManager().save(context);
	}

	private boolean commitFocusedField() {
		for (Map.Entry<NumberSetting, TextFieldWidget> entry : numberFields.entrySet()) {
			TextFieldWidget field = entry.getValue();
			if (field.isFocused()) {
				try {
					double parsed = Double.parseDouble(field.getText().trim());
					entry.getKey().setValue(entry.getKey().clamp(parsed));
					field.setText(formatNumber(entry.getKey()));
					field.setFocused(false);
					context.configManager().save(context);
					return true;
				} catch (NumberFormatException ignored) {
					field.setText(formatNumber(entry.getKey()));
					field.setFocused(false);
					return true;
				}
			}
		}

		for (Map.Entry<StringSetting, TextFieldWidget> entry : stringFields.entrySet()) {
			TextFieldWidget field = entry.getValue();
			if (field.isFocused()) {
				entry.getKey().setValue(field.getText());
				field.setFocused(false);
				context.configManager().save(context);
				return true;
			}
		}
		return false;
	}

	private void syncWidgetsFromSettings() {
		for (Map.Entry<NumberSetting, TextFieldWidget> entry : numberFields.entrySet()) {
			if (!entry.getValue().isFocused()) {
				entry.getValue().setText(formatNumber(entry.getKey()));
			}
		}
		for (Map.Entry<StringSetting, TextFieldWidget> entry : stringFields.entrySet()) {
			if (!entry.getValue().isFocused() && !entry.getValue().getText().equals(entry.getKey().getValue())) {
				entry.getValue().setText(entry.getKey().getValue());
			}
		}
	}

	private String displayItemLabel(String rawValue) {
		if (rawValue != null && rawValue.startsWith("minecraft:")) {
			net.minecraft.util.Identifier identifier = net.minecraft.util.Identifier.tryParse(rawValue);
			if (identifier != null) {
				net.minecraft.block.Block block = net.minecraft.registry.Registries.BLOCK.get(identifier);
				if (block != null && block != net.minecraft.block.Blocks.AIR) {
					return block.getName().getString() + " (" + rawValue + ")";
				}
			}
		}
		Item item = MoneyModuleUtil.resolveItem(rawValue);
		if (item == net.minecraft.item.Items.AIR) {
			return rawValue;
		}
		return item.getName().getString() + " (" + rawValue + ")";
	}

	private boolean usesItemPicker(StringSetting setting) {
		return switch (setting.getId()) {
			case "target_item_id", "player_a_item", "player_b_item", "block_id_1", "block_id_2", "block_id_3", "block_id_4" -> true;
			default -> false;
		};
	}

	private boolean usesBlockPicker(StringSetting setting) {
		return setting.getId().startsWith("block_id_");
	}

	private boolean usesCustomBindRow(StringSetting setting) {
		return isLookPayKeySetting(setting) || isSetHomeKeySetting(setting) || isRigHudToggleKeySetting(setting);
	}

	private boolean isKeyboardMacroBindSetting(StringSetting setting) {
		return module instanceof KeyboardMacrosModule && setting.getId().startsWith("macro_") && setting.getId().endsWith("_key");
	}

	private boolean isMacroTextSetting(StringSetting setting) {
		return module instanceof KeyboardMacrosModule && setting.getId().startsWith("macro_") && setting.getId().endsWith("_text");
	}

	private boolean shouldHideSetting(Setting<?> setting) {
		if (module instanceof KeyboardMacrosModule keyboardMacrosModule) {
			if ("macro_slots".equals(setting.getId())) {
				return true;
			}
			int macroIndex = macroIndexFromSetting(setting);
			if (macroIndex >= keyboardMacrosModule.getActiveMacroCount()) {
				return true;
			}
		}
		return false;
	}

	private boolean usesCrafterGridSetting(StringSetting setting) {
		return module instanceof AutoCraterSlotsModule && "crafter_slots".equals(setting.getId());
	}

	private boolean isLookPayKeySetting(StringSetting setting) {
		return "look_pay_key".equals(setting.getId());
	}

	private boolean isSetHomeKeySetting(StringSetting setting) {
		return "sethome_key".equals(setting.getId());
	}

	private boolean isRigHudToggleKeySetting(StringSetting setting) {
		return "hud_toggle_key".equals(setting.getId());
	}

	private void clearBindingStates() {
		bindingKey = false;
		bindingRigSwitchKey = false;
		bindingRigHudToggleKey = false;
		bindingGambleRemoveTopKey = false;
		bindingClickPayKey = false;
		bindingSetHomeKey = false;
		bindingKeyboardMacroIndex = -1;
	}

	private String bindButtonText(String keyName) {
		return Module.UNBOUND_BIND_TEXT.equals(keyName) ? keyName : "Bind: " + keyName;
	}

	private String displayKeyBinding(KeyBinding keyBinding) {
		if (keyBinding == null || "key.keyboard.unknown".equals(keyBinding.getBoundKeyTranslationKey())) {
			return Module.UNBOUND_BIND_TEXT;
		}
		return keyBinding.getBoundKeyLocalizedText().getString();
	}

	private String formatNumber(NumberSetting setting) {
		return setting.getStep() >= 1.0
			? String.format(Locale.US, "%.0f", setting.getValue())
			: String.format(Locale.US, "%.2f", setting.getValue());
	}

	private void layoutWidgets() {
		int rowY = contentViewportTop() + 6 - contentScroll;
		if (module instanceof KeyboardMacrosModule) {
			rowY += 42;
		}
		if (module instanceof RigModModule) {
			rowY += 42;
		}
		if ("gambles_lister".equals(module.getId())) {
			rowY += 42;
		}
		if ("fake_scoreboard".equals(module.getId())) {
			rowY += 42;
		}

		for (Setting<?> setting : module.getSettings()) {
			if (shouldHideSetting(setting)) {
				continue;
			}
			if (setting instanceof StringSetting stringSetting && usesCrafterGridSetting(stringSetting)) {
				rowY += CRAFTER_GRID_ROW_HEIGHT;
				continue;
			}

			if (setting instanceof StringSetting stringSetting && (usesCustomBindRow(stringSetting) || isKeyboardMacroBindSetting(stringSetting))) {
				rowY += 42;
				continue;
			}

			if (setting instanceof NumberSetting numberSetting) {
				positionField(numberFields.get(numberSetting), left() + 472, rowY + 15, 90, 18, 38);
				rowY += 46;
			} else if (setting instanceof StringSetting stringSetting && !usesItemPicker(stringSetting)) {
				int fieldX = isMacroTextSetting(stringSetting) ? left() + 248 : left() + 280;
				int fieldWidth = isMacroTextSetting(stringSetting) ? 314 : 282;
				positionField(stringFields.get(stringSetting), fieldX, rowY + 14, fieldWidth, 18, 34);
				rowY += 42;
			} else {
				rowY += 42;
			}
		}
	}

	private void positionField(TextFieldWidget field, int x, int y, int width, int height, int rowHeight) {
		if (field == null) {
			return;
		}

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

	private int contentViewportTop() {
		return top() + 106;
	}

	private int contentViewportHeight() {
		return GUI_HEIGHT - 124;
	}

	private int contentHeight() {
		int height = 0;
		if (module instanceof KeyboardMacrosModule) {
			height += 42;
		}
		if (module instanceof RigModModule) {
			height += 42;
		}
		if ("gambles_lister".equals(module.getId())) {
			height += 42;
		}
		if ("fake_scoreboard".equals(module.getId())) {
			height += 42;
		}

		for (Setting<?> setting : module.getSettings()) {
			if (shouldHideSetting(setting)) {
				continue;
			}
			if (setting instanceof StringSetting stringSetting && usesCrafterGridSetting(stringSetting)) {
				height += CRAFTER_GRID_ROW_HEIGHT;
			} else {
				height += setting instanceof NumberSetting ? 46 : 42;
			}
		}
		return height;
	}

	private void renderCrafterGridSetting(DrawContext drawContext, int rowY, StringSetting setting, AutoCraterSlotsModule autoCraterSlotsModule, int textColor, int mutedColor) {
		UiRenderer.drawPanel(drawContext, left() + 20, rowY, 580, CRAFTER_GRID_PANEL_HEIGHT, mutedColor, 0xFF313B34);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, setting.getSection() + " / " + setting.getName(), left() + 32, rowY + 10, 240, textColor);
		UiRenderer.drawWrappedText(
			drawContext,
			textRenderer,
			"Click the 3x3 crafter grid to choose which slots stay enabled. Green slots are the ones this mod auto-selects whenever you open a crafter.",
			left() + 32,
			rowY + 28,
			304,
			5,
			UiRenderer.withAlpha(textColor, 160)
		);

		int frameX = left() + 434;
		int frameY = rowY + 12;
		int frameSize = (CRAFTER_GRID_CELL_SIZE * 3) + (CRAFTER_GRID_CELL_GAP * 2) + 20;
		UiRenderer.drawRoundedPanel(drawContext, frameX, frameY, frameSize, frameSize, 8, 0xFF1E2724, 0xFF3B4941);

		for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
			int column = slotIndex % 3;
			int row = slotIndex / 3;
			int cellX = crafterGridCellX(column);
			int cellY = crafterGridCellY(rowY, row);
			boolean enabled = autoCraterSlotsModule.isConfiguredSlotEnabled(slotIndex);
			int fillColor = enabled ? 0xFF2EC27E : 0xFF24312A;
			int outlineColor = enabled ? 0xFF92F3C7 : 0xFF47544D;
			UiRenderer.drawRoundedPanel(drawContext, cellX, cellY, CRAFTER_GRID_CELL_SIZE, CRAFTER_GRID_CELL_SIZE, 4, fillColor, outlineColor);
		}
	}

	private boolean handleCrafterGridClick(double mouseX, double mouseY, int rowY, AutoCraterSlotsModule autoCraterSlotsModule) {
		for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
			int column = slotIndex % 3;
			int row = slotIndex / 3;
			int cellX = crafterGridCellX(column);
			int cellY = crafterGridCellY(rowY, row);
			if (UiRenderer.isHovered(mouseX, mouseY, cellX, cellY, CRAFTER_GRID_CELL_SIZE, CRAFTER_GRID_CELL_SIZE)) {
				autoCraterSlotsModule.toggleConfiguredSlot(slotIndex);
				return true;
			}
		}
		return false;
	}

	private int crafterGridCellX(int column) {
		return left() + 444 + (column * (CRAFTER_GRID_CELL_SIZE + CRAFTER_GRID_CELL_GAP));
	}

	private int crafterGridCellY(int rowY, int row) {
		return rowY + 22 + (row * (CRAFTER_GRID_CELL_SIZE + CRAFTER_GRID_CELL_GAP));
	}

	private int maxContentScroll() {
		return Math.max(0, contentHeight() - (contentViewportHeight() - 12));
	}

	private int macroIndexFromSetting(Setting<?> setting) {
		String id = setting.getId();
		if (!id.startsWith("macro_")) {
			return -1;
		}

		int secondUnderscore = id.indexOf('_', 6);
		if (secondUnderscore < 0) {
			return -1;
		}

		try {
			return Integer.parseInt(id.substring(6, secondUnderscore)) - 1;
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	private void renderScrollBar(DrawContext drawContext, int x, int y, int height, int scroll, int maxScroll, int accentColor) {
		if (maxScroll <= 0) {
			return;
		}

		UiRenderer.fillRoundedRect(drawContext, x, y, 4, height, 2, 0x44212A28);
		int thumbHeight = Math.max(26, Math.round((height / (float) contentHeight()) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((scroll / (float) maxScroll) * travel);
		UiRenderer.fillRoundedRect(drawContext, x, thumbY, 4, thumbHeight, 2, UiRenderer.withAlpha(accentColor, 220));
	}

	private boolean isScrollThumbHovered(double mouseX, double mouseY) {
		int maxScroll = maxContentScroll();
		if (maxScroll <= 0) {
			return false;
		}

		int x = left() + 604;
		int y = contentViewportTop();
		int height = contentViewportHeight();
		int thumbHeight = Math.max(26, Math.round((height / (float) contentHeight()) * height));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y + Math.round((contentScroll / (float) maxScroll) * travel);
		return UiRenderer.isHovered(mouseX, mouseY, x - 2, thumbY, 10, thumbHeight);
	}

	private void updateScrollFromMouse(double mouseY) {
		int maxScroll = maxContentScroll();
		if (maxScroll <= 0) {
			contentScroll = 0;
			return;
		}

		int trackY = contentViewportTop();
		int trackHeight = contentViewportHeight();
		int thumbHeight = Math.max(26, Math.round((trackHeight / (float) contentHeight()) * trackHeight));
		int travel = Math.max(1, trackHeight - thumbHeight);
		double clamped = MathHelper.clamp(mouseY - trackY - (thumbHeight / 2.0), 0.0, travel);
		contentScroll = Math.round((float) ((clamped / travel) * maxScroll));
		layoutWidgets();
	}

	private int left() {
		return (width - GUI_WIDTH) / 2;
	}

	private int top() {
		return (height - GUI_HEIGHT) / 2;
	}
}
