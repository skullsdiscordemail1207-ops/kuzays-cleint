package com.kuzay2023.client.tab;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public final class VipApiClient {
	private static final Gson GSON = new Gson();
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	private VipApiClient() {
	}

	public static VipClaimResponse claim(String registryUrl, String key, String clientId, String username) throws IOException, InterruptedException {
		VipClaimRequest requestBody = new VipClaimRequest(key, clientId, username);
		return post(registryUrl, "/vip/claim", requestBody, VipClaimResponse.class);
	}

	public static VipCheckResponse check(String registryUrl, String sessionToken, String clientId) throws IOException, InterruptedException {
		VipCheckRequest requestBody = new VipCheckRequest(sessionToken, clientId);
		return post(registryUrl, "/vip/check", requestBody, VipCheckResponse.class);
	}

	private static <T> T post(String registryUrl, String path, Object requestBody, Class<T> responseType) throws IOException, InterruptedException {
		String base = registryUrl == null ? "" : registryUrl.trim();
		if (base.isBlank()) {
			throw new IOException("VIP registry URL is empty.");
		}

		String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		String payload = GSON.toJson(requestBody);
		HttpRequest request = HttpRequest.newBuilder(URI.create(normalized + path))
			.timeout(Duration.ofSeconds(8))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(payload))
			.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("VIP registry returned HTTP " + response.statusCode() + ".");
		}

		try {
			T parsed = GSON.fromJson(response.body(), responseType);
			if (parsed == null) {
				throw new IOException("VIP registry returned an empty response.");
			}
			return parsed;
		} catch (JsonSyntaxException exception) {
			throw new IOException("VIP registry returned invalid JSON.", exception);
		}
	}

	public record VipClaimRequest(String key, String clientId, String username) {
	}

	public record VipCheckRequest(String sessionToken, String clientId) {
	}

	public record VipClaimResponse(boolean ok, String message, String sessionToken, long expiresAt, String display, boolean lifetime) {
	}

	public record VipCheckResponse(boolean ok, String message, boolean valid, long expiresAt, String display, boolean lifetime) {
	}
}
