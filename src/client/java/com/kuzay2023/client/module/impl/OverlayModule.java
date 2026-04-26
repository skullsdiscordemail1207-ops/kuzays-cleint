package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.hud.HudManager;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

public class OverlayModule extends Module {
	private final HudManager hudManager;
	private final BooleanSetting showWatermark;
	private final NumberSetting overlayScale;

	public OverlayModule(HudManager hudManager) {
		super(
			"overlay_manager",
			"Overlay Manager",
			"Controls draggable HUD overlays and their shared scaling.",
			"Other"
		);
		this.hudManager = hudManager;

		showWatermark = addSetting(new BooleanSetting("show_watermark", "Show Watermark", "Elements", true));
		overlayScale = addSetting(new NumberSetting("overlay_scale", "Overlay Scale", "Elements", 1.0, 0.70, 1.80, 0.01));
		setEnabledSilently(true);
	}

	@Override
	public void tick(net.minecraft.client.MinecraftClient client) {
		var watermark = hudManager.getElement("watermark");
		if (watermark != null) {
			watermark.setEnabled(showWatermark.getValue());
		}
	}

	public double getOverlayScale() {
		return overlayScale.getValue();
	}
}
