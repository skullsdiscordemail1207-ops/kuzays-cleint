package com.kuzay2023.client.module.setting;

public class StringSetting extends Setting<String> {
	public StringSetting(String id, String name, String section, String defaultValue) {
		super(id, name, section, defaultValue);
	}

	@Override
	public void deserialize(String value) {
		setValue(value);
	}

	@Override
	public String serialize() {
		return getValue();
	}
}
