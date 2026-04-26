package com.kuzay2023.client.hud;

import java.util.ArrayList;
import java.util.List;

import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.config.ConfigManager;

import net.minecraft.client.gui.DrawContext;

public class HudManager {
	private final ConfigManager configManager;
	private final List<HudElement> elements = new ArrayList<>();

	public HudManager(ConfigManager configManager) {
		this.configManager = configManager;
		register(new WatermarkHudElement());
		register(new GambleListerHudElement());
		register(new HighestGamblersHudElement());
		register(new RigHudElement());
		register(new RealBalanceHudElement());
	}

	public void register(HudElement element) {
		elements.add(element);
	}

	public List<HudElement> getElements() {
		return elements;
	}

	public HudElement getElement(String id) {
		for (HudElement element : elements) {
			if (element.getId().equals(id)) {
				return element;
			}
		}
		return null;
	}

	public void render(DrawContext context) {
		if (KuzayClientModClient.getContext() == null || !configManager.getConfig().layout.showOverlays) {
			return;
		}

		float globalScale = configManager.getConfig().layout.globalScale;
		for (HudElement element : elements) {
			if (!element.isEnabled()) {
				continue;
			}

			context.getMatrices().push();
			context.getMatrices().translate(element.getX(), element.getY(), 0.0F);
			context.getMatrices().scale(element.getScale() * globalScale, element.getScale() * globalScale, 1.0F);
			element.render(context, globalScale);
			context.getMatrices().pop();
		}
	}

	public void save() {
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().configManager().save(KuzayClientModClient.getContext());
		}
	}
}
