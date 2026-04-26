package com.kuzay2023.client;

import com.kuzay2023.client.config.ConfigManager;
import com.kuzay2023.client.hud.HudManager;
import com.kuzay2023.client.module.ModuleManager;
import com.kuzay2023.client.profile.ProfileManager;

import net.minecraft.client.option.KeyBinding;

public record KuzayClientContext(
	ConfigManager configManager,
	ModuleManager moduleManager,
	HudManager hudManager,
	ProfileManager profileManager,
	KeyBinding openGuiKey,
	KeyBinding panicKey,
	KeyBinding rigSwitchSidesKey
) {
}
