package com.kuzay2023.client.hud;

import com.kuzay2023.client.config.HudElementConfig;

import net.minecraft.client.gui.DrawContext;

public abstract class HudElement {
	private final String id;
	private final String name;
	private float x;
	private float y;
	private float scale;
	private boolean enabled;

	protected HudElement(String id, String name, float x, float y) {
		this.id = id;
		this.name = name;
		this.x = x;
		this.y = y;
		this.scale = 1.0F;
		this.enabled = true;
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract void render(DrawContext context, float globalScale);

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean contains(double mouseX, double mouseY, float globalScale) {
		float scaledWidth = getWidth() * scale * globalScale;
		float scaledHeight = getHeight() * scale * globalScale;
		return mouseX >= x && mouseX <= x + scaledWidth && mouseY >= y && mouseY <= y + scaledHeight;
	}

	public HudElementConfig toConfig() {
		HudElementConfig config = new HudElementConfig();
		config.x = x;
		config.y = y;
		config.scale = scale;
		config.enabled = enabled;
		return config;
	}

	public void applyConfig(HudElementConfig config) {
		x = config.x;
		y = config.y;
		scale = config.scale;
		enabled = config.enabled;
	}
}
