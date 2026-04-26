package com.kuzay2023.client.gamble;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.KuzayClientModClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kuzay2023.client.KuzayClientMod;
import com.kuzay2023.client.hud.HudElement;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class GambleListerService {
	private static final Pattern PAYMENT_PATTERN = Pattern.compile("([.]?[A-Za-z0-9_]+)\\s+paid\\s+you\\s+\\$([0-9][0-9,]*(?:\\.\\d+)?(?:[KMBTkmbt])?)", Pattern.CASE_INSENSITIVE);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("skulls_gambles_lister.json");
	private static final int BASE_WIDTH = 170;
	private static final int HEADER_HEIGHT = 14;
	private static final int ROW_HEIGHT = 12;
	private static final int FOOTER_HEIGHT = 16;
	private static final int MIN_SCALE = 50;
	private static final int MAX_SCALE = 250;
	private static final List<PaymentEntry> ENTRIES = new ArrayList<>();

	private static boolean enabled;
	private static boolean initialized;
	private static boolean dragging;
	private static boolean resizing;
	private static boolean listeningForRemoveKey;
	private static double dragOffsetX;
	private static double dragOffsetY;
	private static double resizeOriginX;
	private static double resizeOriginScale;
	private static String lastMatchedMessage = "";
	private static long lastMatchedAt;
	private static PaymentHudState hudState = loadState();
	private static KeyBinding removeTopKey;

	private GambleListerService() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		initialized = true;
		removeTopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.kuzay2023sclient.gamble_lister.remove_top",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"category.kuzay2023sclient.client"
		));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (removeTopKey.wasPressed()) {
				removeTopEntry();
			}
		});
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof ChatScreen)) {
				return;
			}

			ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, mouseX, mouseY, button) -> {
				if (!enabled) {
					return true;
				}
				if (button == 0) {
					PaymentEntry clickedEntry = getEntryAt(mouseX, mouseY);
					if (clickedEntry != null) {
						removeEntry(clickedEntry);
						endInteraction();
						stopListeningForRemoveKey();
						client.setScreen(new ChatScreen(clickedEntry.buildPayCommand()));
						return false;
					}
				}
				return true;
			});
			ScreenMouseEvents.beforeMouseClick(screen).register((currentScreen, mouseX, mouseY, button) -> {
				if (enabled && button == 0) {
					beginInteraction(mouseX, mouseY);
				}
			});
			ScreenMouseEvents.beforeMouseRelease(screen).register((currentScreen, mouseX, mouseY, button) -> {
				if (button == 0) {
					endInteraction();
				}
			});
			ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, key, scancode, modifiers) -> {
				if (!listeningForRemoveKey) {
					return true;
				}

				if (key == GLFW.GLFW_KEY_ESCAPE) {
					stopListeningForRemoveKey();
					return false;
				}

				removeTopKey.setBoundKey(InputUtil.fromKeyCode(key, scancode));
				MinecraftClient.getInstance().options.write();
				stopListeningForRemoveKey();
				return false;
			});
			ScreenEvents.afterRender(screen).register((currentScreen, drawContext, mouseX, mouseY, tickDelta) -> {
				if ((dragging || resizing) && enabled) {
					dragTo(mouseX, mouseY, currentScreen.width, currentScreen.height);
				}
			});
			ScreenEvents.remove(screen).register(currentScreen -> endInteraction());
		});
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

		String normalizedMessage = normalizeMessage(message);
		Matcher matcher = PAYMENT_PATTERN.matcher(normalizedMessage);
		if (!matcher.find()) {
			return;
		}

		long now = System.currentTimeMillis();
		String dedupeKey = matcher.group(1) + "|" + matcher.group(2);
		if (dedupeKey.equals(lastMatchedMessage) && now - lastMatchedAt < 500L) {
			return;
		}

		lastMatchedMessage = dedupeKey;
		lastMatchedAt = now;
		ENTRIES.add(new PaymentEntry(matcher.group(1), matcher.group(2).replace(",", "")));
	}

	public static List<PaymentEntry> entries() {
		return Collections.unmodifiableList(ENTRIES);
	}

	public static int getBaseWidth() {
		return BASE_WIDTH;
	}

	public static int getBaseHeight() {
		return HEADER_HEIGHT + Math.max(ENTRIES.size(), 1) * ROW_HEIGHT + FOOTER_HEIGHT;
	}

	public static int getScaledWidth() {
		return (int) Math.round(BASE_WIDTH * hudState.scale);
	}

	public static int getScaledHeight() {
		return (int) Math.round(getBaseHeight() * hudState.scale);
	}

	public static PaymentHudState getHudState() {
		return hudState;
	}

	public static KeyBinding getRemoveTopKey() {
		return removeTopKey;
	}

	public static boolean isListeningForRemoveKey() {
		return listeningForRemoveKey;
	}

	public static void startListeningForRemoveKey() {
		listeningForRemoveKey = true;
	}

	public static void stopListeningForRemoveKey() {
		listeningForRemoveKey = false;
	}

	public static void setRemoveTopKey(int keyCode, int scanCode) {
		if (removeTopKey == null) {
			return;
		}
		removeTopKey.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
		KeyBinding.updateKeysByCode();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.options.write();
		}
	}

	public static boolean beginInteraction(double mouseX, double mouseY) {
		if (isOverSetKeyButton(mouseX, mouseY)) {
			listeningForRemoveKey = true;
			dragging = false;
			resizing = false;
			return true;
		}

		if (isOverRemoveButton(mouseX, mouseY)) {
			removeTopEntry();
			return true;
		}

		if (isOverResizeHandle(mouseX, mouseY)) {
			resizing = true;
			resizeOriginX = mouseX;
			resizeOriginScale = hudState.scale;
			return true;
		}

		if (contains(mouseX, mouseY)) {
			dragging = true;
			dragOffsetX = mouseX - getHudX();
			dragOffsetY = mouseY - getHudY();
			return true;
		}

		return false;
	}

	public static boolean dragTo(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		boolean changed = false;
		int displayedWidth = getDisplayedWidth();
		int displayedHeight = getDisplayedHeight();
		if (dragging) {
			int nextX = clamp((int) Math.round(mouseX - dragOffsetX), 0, Math.max(0, screenWidth - displayedWidth));
			int nextY = clamp((int) Math.round(mouseY - dragOffsetY), 0, Math.max(0, screenHeight - displayedHeight));
			updateHudPosition(nextX, nextY);
			changed = true;
		}

		if (resizing) {
			double nextScale = resizeOriginScale + ((mouseX - resizeOriginX) / 160.0D);
			hudState.scale = clamp(nextScale, MIN_SCALE / 100.0D, MAX_SCALE / 100.0D);
			updateHudPosition(
				clamp(getHudX(), 0, Math.max(0, screenWidth - getDisplayedWidth())),
				clamp(getHudY(), 0, Math.max(0, screenHeight - getDisplayedHeight()))
			);
			changed = true;
		}

		if (changed) {
			saveHudLayout();
		}

		return changed;
	}

	public static void endInteraction() {
		if (dragging || resizing) {
			saveHudLayout();
		}
		dragging = false;
		resizing = false;
	}

	public static PaymentEntry getEntryAt(double mouseX, double mouseY) {
		if (!contains(mouseX, mouseY) || ENTRIES.isEmpty()) {
			return null;
		}

		double scaledX = getLocalMouseX(mouseX);
		double scaledY = getLocalMouseY(mouseY);
		if (scaledX < 0 || scaledX > BASE_WIDTH) {
			return null;
		}

		if (scaledY < 18 || scaledY >= 18 + (ENTRIES.size() * ROW_HEIGHT)) {
			return null;
		}

		int index = (int) ((scaledY - 18) / ROW_HEIGHT);
		if (index < 0 || index >= ENTRIES.size()) {
			return null;
		}

		return ENTRIES.get(index);
	}

	public static void removeTopEntry() {
		if (!ENTRIES.isEmpty()) {
			ENTRIES.remove(0);
		}
	}

	public static void removeEntry(PaymentEntry entry) {
		ENTRIES.remove(entry);
	}

	private static boolean contains(double mouseX, double mouseY) {
		return mouseX >= getHudX() && mouseX <= getHudX() + getDisplayedWidth()
			&& mouseY >= getHudY() && mouseY <= getHudY() + getDisplayedHeight();
	}

	private static boolean isOverResizeHandle(double mouseX, double mouseY) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}

		double localX = getLocalMouseX(mouseX);
		double localY = getLocalMouseY(mouseY);
		int handleSize = 12;
		return localX >= BASE_WIDTH - handleSize && localX <= BASE_WIDTH
			&& localY >= getBaseHeight() - handleSize && localY <= getBaseHeight();
	}

	private static boolean isOverRemoveButton(double mouseX, double mouseY) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}

		double localX = getLocalMouseX(mouseX);
		double localY = getLocalMouseY(mouseY);
		int buttonHeight = 12;
		int buttonWidth = 56;
		int buttonX = BASE_WIDTH - buttonWidth - 6;
		int buttonY = getBaseHeight() - buttonHeight - 2;
		return localX >= buttonX && localX <= buttonX + buttonWidth && localY >= buttonY && localY <= buttonY + buttonHeight;
	}

	private static boolean isOverSetKeyButton(double mouseX, double mouseY) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}

		double localX = getLocalMouseX(mouseX);
		double localY = getLocalMouseY(mouseY);
		int buttonHeight = 12;
		int buttonWidth = 48;
		int gap = 4;
		int removeWidth = 56;
		int buttonX = BASE_WIDTH - removeWidth - gap - buttonWidth - 6;
		int buttonY = getBaseHeight() - buttonHeight - 2;
		return localX >= buttonX && localX <= buttonX + buttonWidth && localY >= buttonY && localY <= buttonY + buttonHeight;
	}

	private static HudElement getHudElement() {
		return KuzayClientModClient.getContext() == null ? null : KuzayClientModClient.getContext().hudManager().getElement("gamble_lister");
	}

	private static int getHudX() {
		HudElement element = getHudElement();
		return element == null ? hudState.x : Math.round(element.getX());
	}

	private static int getHudY() {
		HudElement element = getHudElement();
		return element == null ? hudState.y : Math.round(element.getY());
	}

	private static double getHudScale() {
		double scale = hudState.scale;
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
		return (int) Math.round(BASE_WIDTH * getHudScale());
	}

	private static int getDisplayedHeight() {
		return (int) Math.round(getBaseHeight() * getHudScale());
	}

	private static double getLocalMouseX(double mouseX) {
		return (mouseX - getHudX()) / getHudScale();
	}

	private static double getLocalMouseY(double mouseY) {
		return (mouseY - getHudY()) / getHudScale();
	}

	private static void updateHudPosition(int x, int y) {
		hudState.x = x;
		hudState.y = y;

		HudElement element = getHudElement();
		if (element != null) {
			element.setX(x);
			element.setY(y);
		}
	}

	private static void saveHudLayout() {
		saveState();
		if (KuzayClientModClient.getContext() != null) {
			KuzayClientModClient.getContext().hudManager().save();
		}
	}

	private static PaymentHudState loadState() {
		if (!Files.exists(CONFIG_PATH)) {
			return new PaymentHudState();
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			PaymentHudState state = GSON.fromJson(reader, PaymentHudState.class);
			if (state == null) {
				return new PaymentHudState();
			}

			state.scale = clamp(state.scale, MIN_SCALE / 100.0D, MAX_SCALE / 100.0D);
			return state;
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.warn("Failed to load gamble lister HUD config", exception);
			return new PaymentHudState();
		}
	}

	private static void saveState() {
		try {
			hudState.x = getHudX();
			hudState.y = getHudY();
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(hudState, writer);
			}
		} catch (IOException exception) {
			KuzayClientMod.LOGGER.warn("Failed to save gamble lister HUD config", exception);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String normalizeMessage(String message) {
		return message.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
	}

	public static final class PaymentHudState {
		public int x = 20;
		public int y = 20;
		public double scale = 1.0D;
	}
}
