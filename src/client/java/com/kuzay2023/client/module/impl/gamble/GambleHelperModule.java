package com.kuzay2023.client.module.impl.gamble;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.gamble.GambleHelperService;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class GambleHelperModule extends Module {
	private static final Pattern USERNAME_PATTERN = Pattern.compile("([.]?[A-Za-z0-9_]{3,16})");

	private final StringSetting lookPayKey = addSetting(new StringSetting("look_pay_key", "Look Pay Key", "General", "key.keyboard.u"));
	private boolean lookPayHeld;

	public GambleHelperModule() {
		super("gamble_helper", "Click Pay", "Makes payment messages clickable and lets you open /pay for the player you are looking at.", "Other");
	}

	@Override
	protected void onEnable() {
		GambleHelperService.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		GambleHelperService.setEnabled(false);
		lookPayHeld = false;
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player == null || client.currentScreen != null) {
			lookPayHeld = false;
			return;
		}

		boolean pressed = isLookPayKeyPressed(client);
		if (pressed && !lookPayHeld) {
			String username = getLookedAtUsername(client);
			if (username != null && !username.isBlank()) {
				client.setScreen(new ChatScreen("/pay " + username + " "));
			}
		}
		lookPayHeld = pressed;
	}

	public String getLookPayKeyName() {
		InputUtil.Key key = InputUtil.fromTranslationKey(lookPayKey.getValue());
		return key == InputUtil.UNKNOWN_KEY ? Module.UNBOUND_BIND_TEXT : key.getLocalizedText().getString();
	}

	public void setLookPayKey(int keyCode, int scanCode) {
		lookPayKey.setValue(keyCode == GLFW.GLFW_KEY_ESCAPE ? "key.keyboard.unknown" : InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey());
		lookPayHeld = false;
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().configManager().save(KuzayClientModClient.getContext());
		}
	}

	private boolean isLookPayKeyPressed(MinecraftClient client) {
		InputUtil.Key key = InputUtil.fromTranslationKey(lookPayKey.getValue());
		return key != InputUtil.UNKNOWN_KEY && InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
	}

	private String getLookedAtUsername(MinecraftClient client) {
		if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
			return null;
		}
		EntityHitResult entityHitResult = (EntityHitResult) client.crosshairTarget;
		if (!(entityHitResult.getEntity() instanceof PlayerEntity playerEntity)) {
			return null;
		}

		PlayerListEntry entry = client.getNetworkHandler() == null ? null : client.getNetworkHandler().getPlayerListEntry(playerEntity.getUuid());
		String profileName = entry != null && entry.getProfile() != null ? entry.getProfile().getName() : null;
		String displayedName = extractPayableUsername(playerEntity.getDisplayName().getString(), profileName);
		if (displayedName != null) {
			return displayedName;
		}

		if (entry != null && entry.getProfile() != null) {
			return entry.getProfile().getName();
		}
		return extractPayableUsername(playerEntity.getName().getString(), null);
	}

	private String extractPayableUsername(String rawName, String preferredBaseName) {
		if (rawName == null) {
			return null;
		}

		String trimmed = rawName.trim();
		Matcher exactMatcher = USERNAME_PATTERN.matcher(trimmed);
		if (exactMatcher.matches()) {
			return exactMatcher.group(1);
		}

		Matcher matcher = USERNAME_PATTERN.matcher(trimmed);
		while (matcher.find()) {
			String candidate = matcher.group(1);
			if (preferredBaseName != null && (candidate.equals(preferredBaseName) || candidate.equals("." + preferredBaseName))) {
				return candidate;
			}
		}

		return null;
	}
}
