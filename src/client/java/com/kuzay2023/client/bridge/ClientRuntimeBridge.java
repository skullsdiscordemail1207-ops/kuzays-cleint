package com.kuzay2023.client.bridge;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.module.bridge.LinkedModule;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Modules;

public final class ClientRuntimeBridge {
	private static final Set<String> EXCLUDED_SECRET_MAIN_MODULES = Set.of(
		"TunnelBaseFinder",
		"PremiumTunnelBaseFinder",
		"FreecamMining",
		"AutoTreeFarmer",
		"AutoPearlChain",
		"RTPBaseFinder",
		"OrderSniper",
		"OrderDropper",
		"AutoTotemOrder",
		"AutoBlazeRodOrder",
		"ShulkerDropper",
		"ShopBuyer",
		"CrateBuyer",
		"AutoShellOrder",
		"AutoSpawnerSell"
	);
	private static final Set<String> BRIDGED_SECRET_VISUAL_MODULES = Set.of(
		"InvisESP"
	);
	private static final Set<String> BRIDGED_CLIENT_RENDER_MODULES = Set.of(
		"ItemPhysics",
		"StorageESP",
		"ESP",
		"Fullbright",
		"Nametags",
		"BlockESP"
	);
	private static final Map<String, String> CATEGORY_OVERRIDES = Map.ofEntries(
		Map.entry("AdminList", "Other"),
		Map.entry("AHSell", "Money"),
		Map.entry("AHSniper", "Money"),
		Map.entry("AutoOrder", "Money"),
		Map.entry("AutoSell", "Money"),
		Map.entry("AutoShulkerOrder", "Money"),
		Map.entry("AutoShulkerShellOrder", "Money"),
		Map.entry("BlazeRodDropper", "Money"),
		Map.entry("CoordSnapper", "Other"),
		Map.entry("CrateBuyer", "Other"),
		Map.entry("EmergencySeller", "Other"),
		Map.entry("HideScoreboard", "Other"),
		Map.entry("HomeReset", "Other"),
		Map.entry("NoBlockInteract", "Other"),
		Map.entry("PlayerDetection", "Other"),
		Map.entry("RainNoti", "Other"),
		Map.entry("RTPEndBaseFinder", "Other"),
		Map.entry("RTPNetherBaseFinder", "Other"),
		Map.entry("RTPer", "Other"),
		Map.entry("SpawnerDropper", "Money"),
		Map.entry("SpawnerOrder", "Money"),
		Map.entry("SpawnerProtect", "Other"),
		Map.entry("ChestAndShulkerStealer", "Other"),
		Map.entry("TabDetector", "Other"),
		Map.entry("TpaAllMacro", "Other"),
		Map.entry("TpaMacro", "Other"),
		Map.entry("UIHelper", "Other"),
		Map.entry("Fullbright", "Other"),
		Map.entry("ItemPhysics", "Other"),
		Map.entry("Nametags", "Other"),
		Map.entry("StorageESP", "Beta"),
		Map.entry("BlockESP", "Beta"),
		Map.entry("ESP", "Beta"),
		Map.entry("InvisESP", "Beta")
	);

	private static boolean registered;

	private ClientRuntimeBridge() {
	}

	public static void registerIfAvailable(KuzayClientContext context) {
		if (registered || context == null) {
			return;
		}

		EmbeddedRuntimeBootstrap.ensureInitialized();
		Modules modules = Modules.get();
		if (modules == null) {
			return;
		}
		Config config = Config.get();
		if (config != null) {
			config.chatFeedback.set(false);
		}

		var bridgeableModules = modules.getAll().stream()
			.filter(ClientRuntimeBridge::isBridgeableModule)
			.sorted(Comparator.comparing(module -> module.title.toLowerCase(Locale.ROOT)))
			.toList();

		if (bridgeableModules.isEmpty()) {
			return;
		}

		for (meteordevelopment.meteorclient.systems.modules.Module meteorModule : bridgeableModules) {
			meteorModule.chatFeedback = false;
			String wrapperId = "secret_" + meteorModule.name;
			if (context.moduleManager().getModule(wrapperId) == null) {
				context.moduleManager().register(new LinkedModule(meteorModule, categoryFor(meteorModule)));
			}
		}

		registered = true;
	}

	public static void applyClientTweaks(KuzayClientContext context) {
		if (context == null || context.configManager().getConfig().runtimeFeedbackDefaultsApplied) {
			return;
		}

		EmbeddedRuntimeBootstrap.ensureInitialized();
		Modules modules = Modules.get();
		if (modules == null) {
			return;
		}
		Config config = Config.get();
		if (config != null) {
			config.chatFeedback.set(false);
		}

		boolean changed = false;
		for (meteordevelopment.meteorclient.systems.modules.Module module : modules.getAll()) {
			if (!isBridgeableModule(module)) {
				continue;
			}

			if (module.chatFeedback) {
				module.chatFeedback = false;
				changed = true;
			}

			for (SettingGroup group : module.settings) {
				for (Setting<?> setting : group) {
					if (setting instanceof BoolSetting boolSetting && isFeedbackSetting(setting) && boolSetting.get()) {
						boolSetting.set(false);
						changed = true;
					}
				}
			}
		}

		context.configManager().getConfig().runtimeFeedbackDefaultsApplied = true;
		if (changed) {
			Systems.save();
		}
		context.configManager().save(context);
	}

	private static boolean isBridgeableModule(meteordevelopment.meteorclient.systems.modules.Module module) {
		return isSecretMainModule(module) || isSecretVisualModule(module) || isClientRenderModule(module);
	}

	private static boolean isSecretMainModule(meteordevelopment.meteorclient.systems.modules.Module module) {
		if (module == null || module.category == null) {
			return false;
		}

		return module.getClass().getName().startsWith("com.nnpg.glazed.modules.main.")
			&& !EXCLUDED_SECRET_MAIN_MODULES.contains(module.getClass().getSimpleName());
	}

	private static boolean isSecretVisualModule(meteordevelopment.meteorclient.systems.modules.Module module) {
		if (module == null || module.category == null) {
			return false;
		}

		return module.getClass().getName().startsWith("com.nnpg.glazed.modules.esp.")
			&& BRIDGED_SECRET_VISUAL_MODULES.contains(module.getClass().getSimpleName());
	}

	private static boolean isClientRenderModule(meteordevelopment.meteorclient.systems.modules.Module module) {
		return module != null
			&& module.category != null
			&& BRIDGED_CLIENT_RENDER_MODULES.contains(module.getClass().getSimpleName())
			&& !module.getClass().getName().startsWith("com.nnpg.glazed.");
	}

	private static String categoryFor(meteordevelopment.meteorclient.systems.modules.Module module) {
		return CATEGORY_OVERRIDES.getOrDefault(module.getClass().getSimpleName(), "VIP");
	}

	private static boolean isFeedbackSetting(Setting<?> setting) {
		String title = setting == null || setting.title == null ? "" : setting.title.toLowerCase(Locale.ROOT);
		return title.contains("notification")
			|| title.contains("chat feedback")
			|| title.contains("chat-feedback");
	}
}
