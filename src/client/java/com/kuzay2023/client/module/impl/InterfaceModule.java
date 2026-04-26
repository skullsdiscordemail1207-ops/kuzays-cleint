package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

public class InterfaceModule extends Module {
	public InterfaceModule() {
		super(
			"interface",
			"Interface",
			"Controls animation, navigation, and panel behavior for the client shell.",
			"Other"
		);

		addSetting(new BooleanSetting("enable_blur", "Blur Backdrop", "Visual", true));
		addSetting(new BooleanSetting("compact_cards", "Compact Cards", "Layout", false));
		addSetting(new NumberSetting("animation_speed", "Animation Speed", "Visual", 0.72, 0.25, 1.50, 0.01));
		addSetting(new EnumSetting("navigation_style", "Navigation Style", "Layout", "Top Bar", "Top Bar", "Pills"));

		setEnabledSilently(true);
	}
}
