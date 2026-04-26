package com.kuzay2023.client.module.setting;

import java.util.List;

public class EnumSetting extends Setting<String> {
	private final List<String> values;

	public EnumSetting(String id, String name, String section, String defaultValue, String... values) {
		super(id, name, section, defaultValue);
		this.values = List.of(values);
	}

	@Override
	public void deserialize(String value) {
		if (values.contains(value)) {
			setValue(value);
		}
	}

	@Override
	public String serialize() {
		return getValue();
	}

	public void cycle() {
		int index = values.indexOf(getValue());
		setValue(values.get((index + 1) % values.size()));
	}
}
