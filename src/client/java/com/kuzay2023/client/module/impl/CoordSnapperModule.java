package com.kuzay2023.client.module.impl;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kuzay2023.client.KuzayClientMod;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class CoordSnapperModule extends Module {
	private final BooleanSetting chatFeedback = addSetting(new BooleanSetting("chat_feedback", "Chat Feedback", "General", false));
	private final BooleanSetting webhook = addSetting(new BooleanSetting("webhook", "Webhook", "Webhook", false));
	private final StringSetting webhookUrl = addSetting(new StringSetting("webhook_url", "Webhook URL", "Webhook", ""));
	private final BooleanSetting selfPing = addSetting(new BooleanSetting("self_ping", "Self Ping", "Webhook", false));
	private final StringSetting discordId = addSetting(new StringSetting("discord_id", "Discord ID", "Webhook", ""));

	public CoordSnapperModule() {
		super("coord_snapper", "Coord Snapper", "Copies coordinates to clipboard + optional webhook", "Other");
	}

	@Override
	protected void onEnable() {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.player == null) {
				setEnabled(false);
				return;
			}

			BlockPos pos = client.player.getBlockPos();
			String coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
			client.keyboard.setClipboard(coords);

			if (webhook.getValue() && !webhookUrl.getValue().trim().isEmpty()) {
				sendWebhook(pos);
			}
		} catch (Exception exception) {
			KuzayClientMod.LOGGER.warn("Coord Snapper failed", exception);
		} finally {
			setEnabled(false);
		}
	}

	private void sendWebhook(BlockPos pos) {
		Thread thread = new Thread(() -> {
			try {
				URL url = new URL(webhookUrl.getValue().trim());
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);

				JsonObject json = new JsonObject();
				json.addProperty("username", "Kuzays Secret Webhook");

				String messageContent = "";
				if (selfPing.getValue() && !discordId.getValue().trim().isEmpty()) {
					messageContent = "<@" + discordId.getValue().trim() + ">";
				}
				json.addProperty("content", messageContent);

				JsonObject embed = new JsonObject();
				embed.addProperty("title", "Coord Snapper Coords");
				embed.addProperty("description", "Coords: X: " + pos.getX() + ", Y: " + pos.getY() + ", Z: " + pos.getZ());
				embed.addProperty("color", 0xE34B4B);
				embed.addProperty("timestamp", Instant.now().toString());

				JsonArray embeds = new JsonArray();
				embeds.add(embed);
				json.add("embeds", embeds);

				try (OutputStream outputStream = connection.getOutputStream()) {
					byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
					outputStream.write(bytes, 0, bytes.length);
				}

				connection.getInputStream().close();
			} catch (Exception exception) {
				KuzayClientMod.LOGGER.warn("Coord Snapper webhook failed", exception);
			}
		}, "kuzays-secret-coord-snapper");
		thread.setDaemon(true);
		thread.start();
	}
}
