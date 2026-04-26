package com.kuzay2023.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class KuzayClientMod implements ModInitializer {
	public static final String MOD_ID = "kuzay2023sclient";
	public static final String DISPLAY_NAME = "kuzay2023's client";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing {} core", DISPLAY_NAME);
	}
}
