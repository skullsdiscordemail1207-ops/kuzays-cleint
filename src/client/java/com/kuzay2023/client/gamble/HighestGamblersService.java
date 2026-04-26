package com.kuzay2023.client.gamble;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.KuzayClientMod;
import com.kuzay2023.client.hud.HudElement;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public final class HighestGamblersService {
	private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
	private static final BigDecimal ONE_BILLION = BigDecimal.valueOf(1_000_000_000L);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Pattern PAYMENT_PATTERN = Pattern.compile("^(?<username>\\S+) paid you \\$(?<amount>[0-9][0-9,]*(?:\\.[0-9]+)?[kKmMbB]?)\\b");
	private static final Path SAVE_PATH = FabricLoader.getInstance().getConfigDir().resolve("skulls_highest_gamblers.json");
	private static final int DEFAULT_HUD_X = 8;
	private static final int DEFAULT_HUD_Y = 8;
	private static final int HUD_WIDTH = 176;
	private static final int LINE_HEIGHT = 12;
	private static final int CLEAR_WIDTH = 42;
	private static final int CLEAR_HEIGHT = 12;
	private static final Map<String, BigDecimal> TOTALS = new LinkedHashMap<>();

	private static boolean initialized;
	private static boolean enabled;
	private static int hudX = DEFAULT_HUD_X;
	private static int hudY = DEFAULT_HUD_Y;
	private static boolean leftMouseDownLastTick;
	private static boolean dragging;
	private static int dragOffsetX;
	private static int dragOffsetY;

	private HighestGamblersService() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		load();
		ClientTickEvents.END_CLIENT_TICK.register(HighestGamblersService::handleClick);
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void handleChatMessage(String message) {
		if (!enabled) {
			return;
		}

		Matcher matcher = PAYMENT_PATTERN.matcher(message.trim());
		if (!matcher.find()) {
			return;
		}

		BigDecimal amount = parseAmount(matcher.group("amount"));
		if (amount != null) {
			TOTALS.merge(matcher.group("username"), amount, BigDecimal::add);
			save();
		}
	}

	public static List<Map.Entry<String, BigDecimal>> getTopThree() {
		return TOTALS.entrySet().stream()
			.sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder())
				.thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
			.limit(3)
			.toList();
	}

	public static void clear() {
		TOTALS.clear();
		save();
	}

	public static String formatAmount(BigDecimal amount) {
		if (amount.compareTo(ONE_BILLION) >= 0) {
			return formatCompact(amount.divide(ONE_BILLION, 2, RoundingMode.DOWN)) + "B";
		}
		if (amount.compareTo(ONE_MILLION) >= 0) {
			return formatCompact(amount.divide(ONE_MILLION, 2, RoundingMode.DOWN)) + "M";
		}
		BigDecimal stripped = amount.stripTrailingZeros();
		return stripped.scale() > 0 ? stripped.setScale(Math.min(stripped.scale(), 2), RoundingMode.DOWN).stripTrailingZeros().toPlainString() : stripped.toPlainString();
	}

	public static int getHudX() {
		HudElement element = getHudElement();
		return element == null ? hudX : Math.round(element.getX());
	}

	public static int getHudY() {
		HudElement element = getHudElement();
		return element == null ? hudY : Math.round(element.getY());
	}

	public static int getHudWidth() {
		return HUD_WIDTH;
	}

	public static int getHudHeight() {
		return 22 + (Math.max(getTopThree().size(), 3) * LINE_HEIGHT) + 8;
	}

	public static int getClearX() {
		return getHudX() + (int) Math.round((HUD_WIDTH - CLEAR_WIDTH - 6) * getHudScale());
	}

	public static int getClearY() {
		return getHudY() + (int) Math.round(5 * getHudScale());
	}

	private static String formatCompact(BigDecimal amount) {
		BigDecimal stripped = amount.stripTrailingZeros();
		return stripped.scale() > 0 ? stripped.toPlainString() : stripped.setScale(0, RoundingMode.DOWN).toPlainString();
	}

	private static BigDecimal parseAmount(String rawAmount) {
		String cleaned = rawAmount.replace(",", "").trim();
		if (cleaned.isEmpty()) {
			return null;
		}

		char suffix = cleaned.charAt(cleaned.length() - 1);
		BigDecimal multiplier = BigDecimal.ONE;
		if (Character.isLetter(suffix)) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
			switch (Character.toLowerCase(suffix)) {
				case 'k' -> multiplier = BigDecimal.valueOf(1_000L);
				case 'm' -> multiplier = BigDecimal.valueOf(1_000_000L);
				case 'b' -> multiplier = BigDecimal.valueOf(1_000_000_000L);
				default -> {
					return null;
				}
			}
		}

		try {
			return new BigDecimal(cleaned).multiply(multiplier);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static void handleClick(MinecraftClient client) {
		if (!enabled || client.player == null) {
			leftMouseDownLastTick = false;
			dragging = false;
			return;
		}

		boolean canInteract = client.currentScreen instanceof ChatScreen;
		if (!canInteract) {
			leftMouseDownLastTick = false;
			dragging = false;
			return;
		}

		long windowHandle = client.getWindow().getHandle();
		boolean actualLeftMouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
		int mouseX = getMouseX(client);
		int mouseY = getMouseY(client);

		if (actualLeftMouseDown && !leftMouseDownLastTick) {
			if (isInsideClearButton(mouseX, mouseY)) {
				clear();
				dragging = false;
			} else if (isInsideHud(mouseX, mouseY)) {
				dragging = true;
				dragOffsetX = mouseX - getHudX();
				dragOffsetY = mouseY - getHudY();
			}
		}

		if (!actualLeftMouseDown) {
			dragging = false;
		} else if (dragging) {
			hudX = mouseX - dragOffsetX;
			hudY = mouseY - dragOffsetY;
			clampHudToWindow(client);
			updateHudPosition(hudX, hudY);
			save();
			saveHudLayout();
		}

		leftMouseDownLastTick = actualLeftMouseDown;
	}

	private static int getMouseX(MinecraftClient client) {
		double scaleX = (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
		return (int) Math.round(client.mouse.getX() * scaleX);
	}

	private static int getMouseY(MinecraftClient client) {
		double scaleY = (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();
		return (int) Math.round(client.mouse.getY() * scaleY);
	}

	private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	private static boolean isInsideHud(int mouseX, int mouseY) {
		return isInside(mouseX, mouseY, getHudX(), getHudY(), getDisplayedWidth(), getDisplayedHeight());
	}

	private static boolean isInsideClearButton(int mouseX, int mouseY) {
		if (!isInsideHud(mouseX, mouseY)) {
			return false;
		}

		double localMouseX = getLocalMouseX(mouseX);
		double localMouseY = getLocalMouseY(mouseY);
		int clearX = HUD_WIDTH - CLEAR_WIDTH - 6;
		int clearY = 5;
		return localMouseX >= clearX && localMouseX <= clearX + CLEAR_WIDTH
			&& localMouseY >= clearY && localMouseY <= clearY + CLEAR_HEIGHT;
	}

	private static void clampHudToWindow(MinecraftClient client) {
		int maxX = Math.max(0, client.getWindow().getScaledWidth() - getDisplayedWidth());
		int maxY = Math.max(0, client.getWindow().getScaledHeight() - getDisplayedHeight());
		hudX = Math.max(0, Math.min(hudX, maxX));
		hudY = Math.max(0, Math.min(hudY, maxY));
	}

	private static HudElement getHudElement() {
		return KuzayClientModClient.getContext() == null ? null : KuzayClientModClient.getContext().hudManager().getElement("highest_gamblers");
	}

	private static double getHudScale() {
		double scale = 1.0D;
		HudElement element = getHudElement();
		if (element != null) {
			scale *= element.getScale();
		}
		if (KuzayClientModClient.getContext() != null) {
			scale *= KuzayClientModClient.getContext().configManager().getConfig().layout.globalScale;
		}
		return scale;
	}

	private static int getDisplayedWidth() {
		return (int) Math.round(HUD_WIDTH * getHudScale());
	}

	private static int getDisplayedHeight() {
		return (int) Math.round(getHudHeight() * getHudScale());
	}

	private static double getLocalMouseX(double mouseX) {
		return (mouseX - getHudX()) / getHudScale();
	}

	private static double getLocalMouseY(double mouseY) {
		return (mouseY - getHudY()) / getHudScale();
	}

	private static void updateHudPosition(int x, int y) {
		hudX = x;
		hudY = y;

		HudElement element = getHudElement();
		if (element != null) {
			element.setX(x);
			element.setY(y);
		}
	}

	private static void saveHudLayout() {
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().hudManager().save();
		}
	}

	private static void load() {
		TOTALS.clear();
		hudX = DEFAULT_HUD_X;
		hudY = DEFAULT_HUD_Y;
		if (!Files.exists(SAVE_PATH)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
			JsonElement root = GSON.fromJson(reader, JsonElement.class);
			if (root == null || root.isJsonNull()) {
				return;
			}
			if (root.isJsonObject() && root.getAsJsonObject().has("totals")) {
				JsonObject object = root.getAsJsonObject();
				if (object.has("hudX")) {
					hudX = object.get("hudX").getAsInt();
				}
				if (object.has("hudY")) {
					hudY = object.get("hudY").getAsInt();
				}
				loadTotals(object.getAsJsonObject("totals"));
			} else if (root.isJsonObject()) {
				loadTotals(root.getAsJsonObject());
			}
			updateHudPosition(hudX, hudY);
		} catch (IOException | JsonSyntaxException exception) {
			KuzayClientMod.LOGGER.error("Failed to load highest gamblers tracker data", exception);
		}
	}

	private static void loadTotals(JsonObject totalsObject) {
		for (Map.Entry<String, JsonElement> entry : totalsObject.entrySet()) {
			try {
				TOTALS.put(entry.getKey(), new BigDecimal(entry.getValue().getAsString()));
			} catch (NumberFormatException exception) {
				KuzayClientMod.LOGGER.warn("Skipping invalid saved amount for {}", entry.getKey(), exception);
			}
		}
	}

	private static void save() {
		try {
			Files.createDirectories(SAVE_PATH.getParent());
			JsonObject root = new JsonObject();
			JsonObject serializedTotals = new JsonObject();
			for (Map.Entry<String, BigDecimal> entry : TOTALS.entrySet()) {
				serializedTotals.addProperty(entry.getKey(), entry.getValue().toPlainString());
			}
			root.addProperty("hudX", getHudX());
			root.addProperty("hudY", getHudY());
			root.add("totals", serializedTotals);

			try (Writer writer = Files.newBufferedWriter(SAVE_PATH)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.error("Failed to save highest gamblers tracker data", exception);
		}
	}
}
