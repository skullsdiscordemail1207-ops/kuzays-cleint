package com.kuzay2023.client.fakescoreboard;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kuzay2023.client.KuzayClientMod;

import net.fabricmc.loader.api.FabricLoader;

public final class FakeScoreboardConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final DecimalFormat WHOLE_FORMAT = new DecimalFormat("#,##0");
	private static final DecimalFormat SHORT_FORMAT = new DecimalFormat("#,##0.##");
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("skulls_fake_scorboard.json");

	private double currentMoney = 0.0D;
	private String title = "SKULLS";
	private List<String> lines = new ArrayList<>(List.of(
		"Ranks",
		"Crates",
		"Store",
		"Balance: {money}"
	));
	private boolean enabled = true;

	public static FakeScoreboardConfig load() {
		if (Files.notExists(CONFIG_PATH)) {
			FakeScoreboardConfig config = new FakeScoreboardConfig();
			config.save();
			return config;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			FakeScoreboardConfig loaded = GSON.fromJson(reader, FakeScoreboardConfig.class);
			if (loaded == null) {
				loaded = new FakeScoreboardConfig();
			}
			loaded.ensureDefaults();
			return loaded;
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.error("Failed to load fake scoreboard config, using defaults", exception);
			return new FakeScoreboardConfig();
		}
	}

	public void save() {
		ensureDefaults();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.error("Failed to save fake scoreboard config", exception);
		}
	}

	public void ensureDefaults() {
		if (title == null || title.isBlank()) {
			title = "SKULLS";
		}
		if (lines == null || lines.isEmpty()) {
			lines = new ArrayList<>(List.of(
				"Ranks",
				"Crates",
				"Store",
				"Balance: {money}"
			));
		}
	}

	public double getCurrentMoney() {
		return currentMoney;
	}

	public void setCurrentMoney(double currentMoney) {
		this.currentMoney = Math.max(0.0D, currentMoney);
	}

	public void changeMoney(double delta) {
		setCurrentMoney(this.currentMoney + delta);
	}

	public String getTitle() {
		return title;
	}

	public List<String> getRenderedLines() {
		ensureDefaults();
		String moneyText = formatMoney(currentMoney);
		List<String> rendered = new ArrayList<>(lines.size());
		for (String line : lines) {
			rendered.add(line.replace("{money}", moneyText));
		}
		return rendered;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public static String formatMoney(double amount) {
		double absolute = Math.abs(amount);
		if (absolute >= 1_000_000_000D) {
			return SHORT_FORMAT.format(amount / 1_000_000_000D) + "B";
		}
		if (absolute >= 1_000_000D) {
			return SHORT_FORMAT.format(amount / 1_000_000D) + "M";
		}
		if (Math.floor(amount) == amount) {
			return WHOLE_FORMAT.format(amount);
		}
		return SHORT_FORMAT.format(amount);
	}
}
