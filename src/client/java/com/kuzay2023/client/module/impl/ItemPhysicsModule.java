package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.NumberSetting;

public class ItemPhysicsModule extends Module {
	private final BooleanSetting onlyOnGround = addSetting(new BooleanSetting("only_on_ground", "Only On Ground", "General", true));
	private final NumberSetting rotationSpeed = addSetting(new NumberSetting("rotation_speed", "Rotation Speed", "General", 1.0, 0.2, 4.0, 0.1));

	public ItemPhysicsModule() {
		super("item_physics", "Item Physics", "Makes dropped items lay flatter and spin with a cleaner Meteor-style look.", "Other");
	}

	public boolean onlyOnGround() {
		return onlyOnGround.getValue();
	}

	public float rotationSpeed() {
		return rotationSpeed.getValue().floatValue();
	}
}
