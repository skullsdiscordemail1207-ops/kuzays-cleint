package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.NumberSetting;

public class AutoReconnectModule extends Module {
	private final NumberSetting delaySeconds = addSetting(new NumberSetting("delay_seconds", "Delay Seconds", "General", 5.0, 1.0, 30.0, 1.0));

	public AutoReconnectModule() {
		super("auto_reconnect", "Auto Reconnect", "Reconnects to the last server after a short disconnect delay.", "Other");
	}

	public int getDelaySeconds() {
		return (int) Math.round(delaySeconds.getValue());
	}
}
