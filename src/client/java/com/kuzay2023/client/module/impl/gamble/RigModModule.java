package com.kuzay2023.client.module.impl.gamble;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.gamble.RigService;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.money.MoneyModuleUtil;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class RigModModule extends Module {
	private final EnumSetting winnerSide = addSetting(new EnumSetting("winner_side", "Winner Side", "General", "WHITE", "WHITE", "YELLOW"));
	private final BooleanSetting showHud = addSetting(new BooleanSetting("show_hud", "Show HUD", "General", true));
	private final StringSetting hudToggleKey = addSetting(new StringSetting("hud_toggle_key", "HUD Toggle Key", "General", "key.keyboard.unknown"));
	private final StringSetting playerAItem = addSetting(new StringSetting("player_a_item", "Player A Item", "Items", "minecraft:white_candle"));
	private final StringSetting playerBItem = addSetting(new StringSetting("player_b_item", "Player B Item", "Items", "minecraft:candle"));
	private boolean hudToggleHeld;

	public RigModModule() {
		super("rig_mod", "Rig Mod", "Spoofs gamble candle numbers and tracks the rigged winner side.", "Gambles");
	}

	public void switchSides() {
		winnerSide.cycle();
	}

	public String getWinnerSideLabel() {
		return winnerSide.getValue();
	}

	public String getHudToggleKeyName() {
		InputUtil.Key key = InputUtil.fromTranslationKey(hudToggleKey.getValue());
		return key == InputUtil.UNKNOWN_KEY ? Module.UNBOUND_BIND_TEXT : key.getLocalizedText().getString();
	}

	public boolean isHudVisibleSetting() {
		return showHud.getValue();
	}

	public void toggleHudVisibility() {
		showHud.setValue(!showHud.getValue());
		RigService.setHudVisible(showHud.getValue());
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().configManager().save(KuzayClientModClient.getContext());
		}
	}

	public void setHudToggleKey(int keyCode, int scanCode) {
		hudToggleKey.setValue(keyCode == GLFW.GLFW_KEY_ESCAPE ? "key.keyboard.unknown" : InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey());
		hudToggleHeld = false;
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().configManager().save(KuzayClientModClient.getContext());
		}
	}

	@Override
	protected void onEnable() {
		RigService.setRigEnabled(true);
	}

	@Override
	protected void onDisable() {
		RigService.setRigEnabled(false);
		hudToggleHeld = false;
	}

	@Override
	public void tick(MinecraftClient client) {
		boolean togglePressed = isHudToggleKeyPressed(client);
		if (togglePressed && !hudToggleHeld && client.currentScreen == null) {
			showHud.setValue(!showHud.getValue());
			if (KuzayClientModClient.getContext() != null) {
				KuzayClientModClient.getContext().configManager().save(KuzayClientModClient.getContext());
			}
		}
		hudToggleHeld = togglePressed;

		RigService.setHudVisible(showHud.getValue());
		RigService.setWinnerSide("YELLOW".equals(winnerSide.getValue()) ? RigService.WinnerSide.YELLOW : RigService.WinnerSide.WHITE);
		RigService.setPlayerAItem(MoneyModuleUtil.resolveItem(playerAItem.getValue()));
		RigService.setPlayerBItem(MoneyModuleUtil.resolveItem(playerBItem.getValue()));
		RigService.tick(client);
	}

	private boolean isHudToggleKeyPressed(MinecraftClient client) {
		if (client == null) {
			return false;
		}
		InputUtil.Key key = InputUtil.fromTranslationKey(hudToggleKey.getValue());
		return key != InputUtil.UNKNOWN_KEY && InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
	}
}
