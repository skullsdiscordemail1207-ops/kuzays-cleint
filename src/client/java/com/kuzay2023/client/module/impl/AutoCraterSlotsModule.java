package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CrafterScreen;
import net.minecraft.screen.CrafterScreenHandler;

public class AutoCraterSlotsModule extends Module {
	private static final int TOTAL_CRAFTER_SLOTS = 9;
	private static final String DEFAULT_SLOT_PATTERN = "111111111";

	private final StringSetting crafterSlots = addSetting(new StringSetting("crafter_slots", "Crafter Slots", "Crafter", DEFAULT_SLOT_PATTERN));

	public AutoCraterSlotsModule() {
		super("auto_crater_slots", "Auto Crater Slots", "Automatically applies your selected 3x3 crafter slot layout whenever you open a crafter.", "Other");
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client == null || client.player == null || client.interactionManager == null) {
			return;
		}
		if (!(client.currentScreen instanceof CrafterScreen screen)) {
			return;
		}

		applyCrafterLayout(client, screen.getScreenHandler());
	}

	public boolean isConfiguredSlotEnabled(int slotIndex) {
		if (slotIndex < 0 || slotIndex >= TOTAL_CRAFTER_SLOTS) {
			return false;
		}
		return getNormalizedPattern().charAt(slotIndex) == '1';
	}

	public void toggleConfiguredSlot(int slotIndex) {
		if (slotIndex < 0 || slotIndex >= TOTAL_CRAFTER_SLOTS) {
			return;
		}

		char[] slots = getNormalizedPattern().toCharArray();
		slots[slotIndex] = slots[slotIndex] == '1' ? '0' : '1';
		crafterSlots.setValue(new String(slots));
	}

	private void applyCrafterLayout(MinecraftClient client, CrafterScreenHandler handler) {
		for (int slotIndex = 0; slotIndex < TOTAL_CRAFTER_SLOTS; slotIndex++) {
			boolean shouldEnable = isConfiguredSlotEnabled(slotIndex);
			boolean currentlyEnabled = !handler.isSlotDisabled(slotIndex);
			if (currentlyEnabled == shouldEnable) {
				continue;
			}

			handler.setSlotEnabled(slotIndex, shouldEnable);
			client.interactionManager.slotChangedState(slotIndex, handler.syncId, shouldEnable);
		}
	}

	private String getNormalizedPattern() {
		String raw = crafterSlots.getValue();
		if (raw == null) {
			raw = "";
		}

		StringBuilder normalized = new StringBuilder(TOTAL_CRAFTER_SLOTS);
		for (int slotIndex = 0; slotIndex < TOTAL_CRAFTER_SLOTS; slotIndex++) {
			char fallback = DEFAULT_SLOT_PATTERN.charAt(slotIndex);
			char current = slotIndex < raw.length() ? raw.charAt(slotIndex) : fallback;
			normalized.append(current == '0' ? '0' : '1');
		}

		String pattern = normalized.toString();
		if (!pattern.equals(raw)) {
			crafterSlots.setValue(pattern);
		}
		return pattern;
	}
}
