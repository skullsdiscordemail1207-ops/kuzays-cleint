package com.kuzay2023.client.module.impl;

import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.setting.BooleanSetting;
import com.kuzay2023.client.module.setting.EnumSetting;
import com.kuzay2023.client.module.setting.NumberSetting;
import com.kuzay2023.client.module.setting.StringSetting;

public class SimplePlaceholderModule extends Module {
	public SimplePlaceholderModule(String id, String name, String description, String category) {
		super(id, name, description, category);
		addCommonSettings();
		addModuleSpecificSettings(id);
	}

	private void addCommonSettings() {
		addSetting(new BooleanSetting("notifications", "Notifications", "General", true));
		addSetting(new NumberSetting("delay_ticks", "Delay Ticks", "General", 5.0, 0.0, 40.0, 1.0));
		addSetting(new StringSetting("notes", "Notes", "General", ""));
	}

	private void addModuleSpecificSettings(String id) {
		switch (id) {
			case "ah_sniper" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:diamond"));
				addSetting(new StringSetting("max_price", "Max Price", "General", "1m"));
				addSetting(new NumberSetting("refresh_delay", "Refresh Delay", "General", 6.0, 0.0, 40.0, 1.0));
			}
			case "auto_order" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:obsidian"));
				addSetting(new StringSetting("order_price", "Order Price", "General", "30k"));
				addSetting(new NumberSetting("order_amount", "Order Amount", "General", 64.0, 1.0, 2304.0, 1.0));
			}
			case "auto_sell" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:diamond"));
				addSetting(new StringSetting("sell_price", "Sell Price", "General", "30k"));
				addSetting(new BooleanSetting("only_hotbar", "Only Hotbar", "General", true));
			}
			case "auto_shulker_order" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:shulker_box"));
				addSetting(new StringSetting("order_price", "Order Price", "General", "250k"));
				addSetting(new NumberSetting("order_amount", "Order Amount", "General", 16.0, 1.0, 256.0, 1.0));
			}
			case "auto_shulker_shell_order" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:shulker_shell"));
				addSetting(new StringSetting("order_price", "Order Price", "General", "20k"));
				addSetting(new NumberSetting("order_amount", "Order Amount", "General", 64.0, 1.0, 2304.0, 1.0));
			}
			case "blaze_rod_dropper", "spawner_dropper" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", id.equals("blaze_rod_dropper") ? "minecraft:blaze_rod" : "minecraft:spawner"));
				addSetting(new NumberSetting("drop_amount", "Drop Amount", "General", 64.0, 1.0, 2304.0, 1.0));
				addSetting(new BooleanSetting("drop_all", "Drop All", "General", true));
			}
			case "spawner_order" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:spawner"));
				addSetting(new StringSetting("order_price", "Order Price", "General", "5m"));
				addSetting(new NumberSetting("order_amount", "Order Amount", "General", 16.0, 1.0, 256.0, 1.0));
			}
			case "admin_list", "tab_detector" -> {
				addSetting(new EnumSetting("mode", "Mode", "General", "Compact", "Compact", "Detailed", "Silent"));
				addSetting(new BooleanSetting("copy_names", "Copy Names", "General", false));
			}
			case "emergency_seller" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", "minecraft:totem_of_undying"));
				addSetting(new StringSetting("sell_price", "Sell Price", "General", "100k"));
				addSetting(new NumberSetting("trigger_count", "Trigger Count", "General", 1.0, 1.0, 64.0, 1.0));
			}
			case "home_reset" -> {
				addSetting(new StringSetting("home_name", "Home Name", "General", "home"));
				addSetting(new NumberSetting("reset_delay", "Reset Delay", "General", 20.0, 0.0, 200.0, 1.0));
			}
			case "no_block_interact" -> {
				addSetting(new EnumSetting("mode", "Mode", "General", "Strict", "Strict", "Containers Only", "Whitelist"));
				addSetting(new BooleanSetting("allow_chests", "Allow Chests", "Whitelist", false));
				addSetting(new BooleanSetting("allow_doors", "Allow Doors", "Whitelist", false));
			}
			case "rtp_end_base_finder", "rtp_nether_base_finder", "rtper" -> {
				addSetting(new NumberSetting("interval_seconds", "Interval Seconds", "General", 5.0, 1.0, 300.0, 1.0));
				addSetting(new NumberSetting("max_runs", "Max Runs", "General", 50.0, 1.0, 500.0, 1.0));
			}
			case "spawner_protect" -> {
				addSetting(new BooleanSetting("protect_nearby", "Protect Nearby", "General", true));
				addSetting(new NumberSetting("protect_radius", "Protect Radius", "General", 6.0, 1.0, 16.0, 1.0));
			}
			case "storage_stealer" -> {
				addSetting(new NumberSetting("steal_delay", "Steal Delay", "General", 2.0, 0.0, 20.0, 1.0));
				addSetting(new BooleanSetting("auto_close", "Auto Close", "General", true));
				addSetting(new StringSetting("target_item_id", "Priority Item", "General", "minecraft:shulker_box"));
			}
			case "tpa_all_macro", "tpa_macro" -> {
				addSetting(new StringSetting("target_name", "Target Name", "General", ".username"));
				addSetting(new NumberSetting("command_delay", "Command Delay", "General", 10.0, 0.0, 100.0, 1.0));
			}
			case "ui_helper" -> {
				addSetting(new BooleanSetting("snap_windows", "Snap Windows", "General", true));
				addSetting(new BooleanSetting("hide_tooltips", "Hide Tooltips", "General", false));
			}
			case "item_physics" -> {
				addSetting(new EnumSetting("mode", "Mode", "General", "Smooth", "Smooth", "Strong", "Arcade"));
				addSetting(new NumberSetting("spin_speed", "Spin Speed", "General", 1.0, 0.1, 5.0, 0.1));
			}
			case "auto_shell_order", "auto_spawner_sell" -> {
				addSetting(new StringSetting("target_item_id", "Target Item", "General", id.equals("auto_shell_order") ? "minecraft:shulker_shell" : "minecraft:spawner"));
				addSetting(new StringSetting("price", "Price", "General", id.equals("auto_shell_order") ? "20k" : "5m"));
				addSetting(new NumberSetting("amount", "Amount", "General", 64.0, 1.0, 2304.0, 1.0));
			}
			default -> addSetting(new EnumSetting("mode", "Mode", "General", "Basic", "Basic", "Normal", "Fast", "Safe"));
		}
	}
}
