package com.kuzay2023.client.profile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientMod;
import com.kuzay2023.client.config.ClientConfig;
import com.kuzay2023.client.gamble.GambleListerService;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class ProfileManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path profilePath = FabricLoader.getInstance().getConfigDir().resolve("kuzay2023sclient-profiles.json");
	private ProfileState state = new ProfileState();

	public void load() {
		if (!Files.exists(profilePath)) {
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(profilePath)) {
			ProfileState loaded = GSON.fromJson(reader, ProfileState.class);
			if (loaded != null) {
				state = loaded;
			}
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.error("Failed to load profiles", exception);
		}
	}

	public List<ProfileEntry> getProfiles() {
		return new ArrayList<>(state.profiles);
	}

	public ProfileEntry getProfile(String profileName) {
		String normalized = normalize(profileName);
		for (ProfileEntry profile : state.profiles) {
			if (normalize(profile.name).equals(normalized)) {
				return profile;
			}
		}
		return null;
	}

	public boolean saveProfile(String profileName, KuzayClientContext context) {
		String trimmedName = profileName == null ? "" : profileName.trim();
		if (trimmedName.isEmpty()) {
			return false;
		}

		ProfileEntry profile = getProfile(trimmedName);
		if (profile == null) {
			profile = new ProfileEntry();
			profile.name = trimmedName;
			state.profiles.add(profile);
		}
		profile.name = trimmedName;
		profile.config = context.configManager().capture(context);
		save();
		return true;
	}

	public boolean loadProfile(String profileName, KuzayClientContext context) {
		ProfileEntry profile = getProfile(profileName);
		if (profile == null) {
			return false;
		}

		context.configManager().apply(context, profile.config);
		applyGlobalKeys(context, profile.config);
		return true;
	}

	public boolean deleteProfile(String profileName) {
		ProfileEntry profile = getProfile(profileName);
		if (profile == null) {
			return false;
		}

		state.profiles.remove(profile);
		if (normalize(profile.name).equals(normalize(state.autoLoadProfileName))) {
			state.autoLoadProfileName = "";
		}
		save();
		return true;
	}

	public boolean isAutoLoadEnabled() {
		return state.autoLoadEnabled;
	}

	public void setAutoLoadEnabled(boolean enabled) {
		state.autoLoadEnabled = enabled;
		save();
	}

	public String getAutoLoadProfileName() {
		return state.autoLoadProfileName == null ? "" : state.autoLoadProfileName;
	}

	public void setAutoLoadProfileName(String profileName) {
		state.autoLoadProfileName = profileName == null ? "" : profileName.trim();
		save();
	}

	public void autoLoadForCurrentServer(KuzayClientContext context) {
		if (!state.autoLoadEnabled || state.autoLoadProfileName == null || state.autoLoadProfileName.isBlank()) {
			return;
		}
		loadProfile(state.autoLoadProfileName, context);
	}

	private void applyGlobalKeys(KuzayClientContext context, ClientConfig config) {
		setKey(context.openGuiKey(), config.openGuiKeyTranslation);
		setKey(context.panicKey(), config.panicKeyTranslation);
		setKey(context.rigSwitchSidesKey(), config.rigSwitchSidesKeyTranslation);
		if (GambleListerService.getRemoveTopKey() != null) {
			setKey(GambleListerService.getRemoveTopKey(), config.gambleListerRemoveTopKeyTranslation);
		}
		KeyBinding.updateKeysByCode();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.options.write();
		}
		context.configManager().save(context);
	}

	private void setKey(KeyBinding keyBinding, String translationKey) {
		String nextKey = translationKey == null || translationKey.isBlank() ? "key.keyboard.unknown" : translationKey;
		keyBinding.setBoundKey(InputUtil.fromTranslationKey(nextKey));
	}

	private void save() {
		try {
			Files.createDirectories(profilePath.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(profilePath)) {
				GSON.toJson(state, writer);
			}
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.error("Failed to save profiles", exception);
		}
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
