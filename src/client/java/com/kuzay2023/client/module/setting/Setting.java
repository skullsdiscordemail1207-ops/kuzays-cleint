package com.kuzay2023.client.module.setting;

public abstract class Setting<T> {
	private final String id;
	private final String name;
	private final String section;
	private T value;

	protected Setting(String id, String name, String section, T defaultValue) {
		this.id = id;
		this.name = name;
		this.section = section;
		this.value = defaultValue;
	}

	public abstract void deserialize(String value);

	public abstract String serialize();

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSection() {
		return section;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}
}
