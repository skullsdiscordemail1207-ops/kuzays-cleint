package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.StringSetting;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BlockEspModule extends Module {
	private final NumberSetting renderRadius = addSetting(new NumberSetting("render_radius", "Render Radius", "General", 20.0, 4.0, 48.0, 1.0));

	private final BooleanSetting slotOneEnabled = addSetting(new BooleanSetting("slot_1_enabled", "Slot 1 Enabled", "Selections", true));
	private final StringSetting blockOneId = addSetting(new StringSetting("block_id_1", "Block 1", "Selections", "minecraft:spawner"));
	private final EnumSetting blockOneColor = addSetting(new EnumSetting("block_color_1", "Block 1 Color", "Colors", "Red", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));

	private final BooleanSetting slotTwoEnabled = addSetting(new BooleanSetting("slot_2_enabled", "Slot 2 Enabled", "Selections", false));
	private final StringSetting blockTwoId = addSetting(new StringSetting("block_id_2", "Block 2", "Selections", "minecraft:beacon"));
	private final EnumSetting blockTwoColor = addSetting(new EnumSetting("block_color_2", "Block 2 Color", "Colors", "Cyan", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));

	private final BooleanSetting slotThreeEnabled = addSetting(new BooleanSetting("slot_3_enabled", "Slot 3 Enabled", "Selections", false));
	private final StringSetting blockThreeId = addSetting(new StringSetting("block_id_3", "Block 3", "Selections", "minecraft:chest"));
	private final EnumSetting blockThreeColor = addSetting(new EnumSetting("block_color_3", "Block 3 Color", "Colors", "Yellow", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));

	private final BooleanSetting slotFourEnabled = addSetting(new BooleanSetting("slot_4_enabled", "Slot 4 Enabled", "Selections", false));
	private final StringSetting blockFourId = addSetting(new StringSetting("block_id_4", "Block 4", "Selections", "minecraft:ender_chest"));
	private final EnumSetting blockFourColor = addSetting(new EnumSetting("block_color_4", "Block 4 Color", "Colors", "Purple", "Red", "Green", "Blue", "Yellow", "Cyan", "Orange", "Pink", "White", "Purple"));

	public BlockEspModule() {
		super("block_esp", "Block ESP", "Highlights selected blocks with separate outline colors for each slot.", "Other");
	}

	public int getRenderRadius() {
		return (int) Math.round(renderRadius.getValue());
	}

	public String getColorName(BlockState state) {
		String id = Registries.BLOCK.getId(state.getBlock()).toString();
		if (slotOneEnabled.getValue() && id.equals(blockOneId.getValue())) return blockOneColor.getValue();
		if (slotTwoEnabled.getValue() && id.equals(blockTwoId.getValue())) return blockTwoColor.getValue();
		if (slotThreeEnabled.getValue() && id.equals(blockThreeId.getValue())) return blockThreeColor.getValue();
		if (slotFourEnabled.getValue() && id.equals(blockFourId.getValue())) return blockFourColor.getValue();
		return "White";
	}

	public boolean shouldRender(BlockState state) {
		String id = Registries.BLOCK.getId(state.getBlock()).toString();
		return matches(slotOneEnabled, blockOneId, id)
			|| matches(slotTwoEnabled, blockTwoId, id)
			|| matches(slotThreeEnabled, blockThreeId, id)
			|| matches(slotFourEnabled, blockFourId, id);
	}

	private boolean matches(BooleanSetting enabled, StringSetting blockId, String currentId) {
		if (!enabled.getValue()) {
			return false;
		}
		String configuredId = normalizeBlockId(blockId.getValue());
		return !configuredId.isBlank() && configuredId.equals(currentId);
	}

	private String normalizeBlockId(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		Identifier identifier = Identifier.tryParse(raw.trim().toLowerCase());
		return identifier == null ? "" : identifier.toString();
	}
}
