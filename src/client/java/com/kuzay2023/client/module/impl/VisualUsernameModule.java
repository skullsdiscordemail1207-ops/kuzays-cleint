package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.StringSetting;

public class VisualUsernameModule extends Module {
	private final StringSetting visualUsername = addSetting(new StringSetting("visual_username", "Visual Username", "General", ""));

	public VisualUsernameModule() {
		super("visual_username", "Visual Username", "Changes your client-side displayed username without changing your real account name.", "Other");
	}

	public String getVisualUsername() {
		return visualUsername.getValue();
	}
}
