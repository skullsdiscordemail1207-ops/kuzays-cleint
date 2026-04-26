package com.kuzay2023.client.module;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.module.setting.Setting;
import com.kuzay2023.client.tab.VipAccessManager;
import com.kuzay2023.client.tab.ClientTab;
import com.kuzay2023.client.tab.ServerTabManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public abstract class Module {
	public static final String UNBOUND_BIND_TEXT = "Unbound click to bind";
	public static final int MOUSE_BUTTON_OFFSET = 1000;

	private final String id;
	private final String name;
	private final String description;
	private final String category;
	private String categoryOverride;
	private final Set<String> extraCategories = new LinkedHashSet<>();
	private boolean originalCategoryHidden;
	private boolean vipOnly;
	private final List<Setting<?>> settings = new ArrayList<>();
	private SafetyLevel safetyLevel = SafetyLevel.NONE;

	private boolean enabled;
	private boolean hidden;
	private int boundKey = GLFW.GLFW_KEY_UNKNOWN;
	private boolean keyHeld;
	private long scheduledEnableAt = -1L;

	protected Module(String id, String name, String description, String category) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <T extends Setting<?>> T addSetting(T setting) {
		settings.add(setting);
		return setting;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if (enabled && vipOnly && !VipAccessManager.isUnlocked()) {
			return;
		}
		scheduledEnableAt = -1L;
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void setEnabledSilently(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public void restartAfterDelay(long delayMillis) {
		setEnabled(false);
		scheduledEnableAt = System.currentTimeMillis() + Math.max(0L, delayMillis);
	}

	public int getBoundKey() {
		return boundKey;
	}

	public void setBoundKey(int boundKey) {
		this.boundKey = boundKey;
	}

	public void setBoundKeySilently(int boundKey) {
		this.boundKey = boundKey;
	}

	public String getBoundKeyName() {
		return describeBoundKey(boundKey);
	}

	public void updateKeybind(MinecraftClient client) {
		if (boundKey == GLFW.GLFW_KEY_UNKNOWN) {
			keyHeld = false;
			return;
		}

		boolean pressed = isBindPressed(client, boundKey);
		if (client.currentScreen != null && !isCtrlDown(client)) {
			pressed = false;
		}
		if (pressed && !keyHeld) {
			toggle();
		}

		keyHeld = pressed;
	}

	private boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	public void tick(MinecraftClient client) {
	}

	public void tickScheduled(MinecraftClient client) {
		if (!enabled && scheduledEnableAt > 0L && System.currentTimeMillis() >= scheduledEnableAt) {
			scheduledEnableAt = -1L;
			setEnabled(true);
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}

	public String getOriginalCategory() {
		return category;
	}

	public void setCategoryOverride(String categoryOverride) {
		this.categoryOverride = categoryOverride;
	}

	public String getCategoryOverride() {
		return categoryOverride;
	}

	public Set<String> getExtraCategories() {
		return new LinkedHashSet<>(extraCategories);
	}

	public void clearExtraCategories() {
		extraCategories.clear();
	}

	public boolean isOriginalCategoryHidden() {
		return originalCategoryHidden;
	}

	public void setOriginalCategoryHidden(boolean originalCategoryHidden) {
		this.originalCategoryHidden = originalCategoryHidden;
	}

	public boolean isVipOnly() {
		return vipOnly;
	}

	public void setVipOnly(boolean vipOnly) {
		this.vipOnly = vipOnly;
	}

	public void addExtraCategory(String category) {
		if (category == null || category.isBlank() || this.category.equals(category)) {
			return;
		}
		extraCategories.add(category);
	}

	public void removeExtraCategory(String category) {
		if (category == null) {
			return;
		}
		extraCategories.remove(category);
	}

	public boolean hasExtraCategory(String category) {
		return category != null && extraCategories.contains(category);
	}

	public boolean belongsToCategory(String category) {
		if (category == null || category.isBlank() || "All".equals(category)) {
			return true;
		}
		return this.category.equals(category) || extraCategories.contains(category);
	}

	public boolean belongsToTab(ClientTab tab) {
		if (tab == null || tab.all()) {
			return true;
		}
		if (tab.isGlobal()) {
			return (!originalCategoryHidden && category.equals(tab.baseCategory()))
				|| extraCategories.contains(tab.id())
				|| extraCategories.contains(tab.baseCategory());
		}
		return extraCategories.contains(tab.id())
			|| ((!originalCategoryHidden && category.equals(tab.baseCategory())) && tab.serverKey() != null && !tab.serverKey().isBlank());
	}

	public boolean hasExplicitTabAssignment(ClientTab tab) {
		if (tab == null || tab.all()) {
			return false;
		}
		if (tab.isGlobal()) {
			return extraCategories.contains(tab.id()) || extraCategories.contains(tab.baseCategory());
		}
		return extraCategories.contains(tab.id());
	}

	public boolean canRemoveFromTab(ClientTab tab) {
		if (tab == null || tab.all()) {
			return false;
		}
		if (tab.isGlobal() && category.equals(tab.baseCategory()) && !originalCategoryHidden) {
			return true;
		}
		return hasExplicitTabAssignment(tab);
	}

	public void assignToTab(ClientTab tab) {
		if (tab == null || tab.all()) {
			return;
		}
		if (tab.isGlobal() && category.equals(tab.baseCategory())) {
			originalCategoryHidden = false;
			return;
		}
		extraCategories.add(tab.id());
	}

	public void removeFromTab(ClientTab tab) {
		if (tab == null || tab.all()) {
			return;
		}
		extraCategories.remove(tab.id());
		if (tab.isGlobal()) {
			if (category.equals(tab.baseCategory())) {
				originalCategoryHidden = true;
				return;
			}
			extraCategories.remove(tab.baseCategory());
		}
	}

	public List<Setting<?>> getSettings() {
		return settings;
	}

	public SafetyLevel getSafetyLevel() {
		return safetyLevel;
	}

	public void setSafetyLevel(SafetyLevel safetyLevel) {
		this.safetyLevel = safetyLevel == null ? SafetyLevel.NONE : safetyLevel;
	}

	public static String describeBoundKey(int boundKey) {
		if (boundKey == GLFW.GLFW_KEY_UNKNOWN) {
			return UNBOUND_BIND_TEXT;
		}
		if (isMouseBinding(boundKey)) {
			return mouseButtonName(getMouseButton(boundKey));
		}
		return InputUtil.fromKeyCode(boundKey, 0).getLocalizedText().getString();
	}

	public static boolean isMouseBinding(int boundKey) {
		return boundKey >= MOUSE_BUTTON_OFFSET;
	}

	public static int encodeMouseButton(int mouseButton) {
		return MOUSE_BUTTON_OFFSET + Math.max(0, mouseButton);
	}

	public static int getMouseButton(int boundKey) {
		return Math.max(0, boundKey - MOUSE_BUTTON_OFFSET);
	}

	protected static boolean isBindPressed(MinecraftClient client, int boundKey) {
		long windowHandle = client.getWindow().getHandle();
		if (isMouseBinding(boundKey)) {
			return GLFW.glfwGetMouseButton(windowHandle, getMouseButton(boundKey)) == GLFW.GLFW_PRESS;
		}
		return InputUtil.isKeyPressed(windowHandle, boundKey);
	}

	private static String mouseButtonName(int mouseButton) {
		return switch (mouseButton) {
			case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "Mouse Left";
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Mouse Right";
			case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Mouse Middle";
			case GLFW.GLFW_MOUSE_BUTTON_4 -> "Mouse 4";
			case GLFW.GLFW_MOUSE_BUTTON_5 -> "Mouse 5";
			default -> "Mouse " + (mouseButton + 1);
		};
	}

	public enum SafetyLevel {
		NONE,
		SAFE,
		RISKY,
		BANNABLE
	}
}
