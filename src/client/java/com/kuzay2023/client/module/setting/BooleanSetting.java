package com.kuzay2023.client.module.setting;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String id, String name, String section, boolean defaultValue) {
		super(id, name, section, defaultValue);
	}

	@Override
	public void deserialize(String value) {
		setValue(Boolean.parseBoolean(value));
	}

	@Override
	public String serialize() {
		return Boolean.toString(getValue());
	}
}
