package com.kuzay2023.client.module.setting;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String id, String name, String section, double defaultValue, double min, double max, double step) {
		super(id, name, section, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	@Override
	public void deserialize(String value) {
		try {
			setValue(clamp(Double.parseDouble(value)));
		} catch (NumberFormatException ignored) {
		}
	}

	@Override
	public String serialize() {
		return String.format(Locale.US, "%.2f", getValue());
	}

	public double clamp(double value) {
		double clamped = Math.max(min, Math.min(max, value));
		return Math.round(clamped / step) * step;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}
}
