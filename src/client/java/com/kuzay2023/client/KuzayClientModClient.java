package com.kuzay2023.client;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.config.ConfigManager;
import com.kuzay2023.client.balance.BalanceHooks;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardClientHooks;
import com.kuzay2023.client.gamble.GambleHelperService;
import com.kuzay2023.client.gamble.GambleListerService;
import com.kuzay2023.client.gamble.HighestGamblersService;
import com.kuzay2023.client.gui.ClientScreen;
import com.kuzay2023.client.gui.HudEditorScreen;
import com.kuzay2023.client.hud.HudManager;
import com.kuzay2023.client.network.AutoReconnectManager;
import com.kuzay2023.client.network.AutoReconnectScreenHooks;
import com.kuzay2023.client.render.EspRenderManager;
import com.kuzay2023.client.module.ModuleManager;
import com.kuzay2023.client.profile.ProfileManager;
import com.kuzay2023.client.tab.ServerTabManager;
import com.kuzay2023.client.tab.VipAccessManager;
import com.kuzay2023.client.module.impl.AutoCraterSlotsModule;
import com.kuzay2023.client.module.impl.AutoReconnectModule;
import com.kuzay2023.client.module.impl.BlockEspModule;
import com.kuzay2023.client.module.impl.CoordSnapperModule;
import com.kuzay2023.client.module.impl.EspModule;
import com.kuzay2023.client.module.impl.FakeBalanceModule;
import com.kuzay2023.client.module.impl.FullbrightModule;
import com.kuzay2023.client.module.impl.FreeLookModule;
import com.kuzay2023.client.module.impl.FreecamModule;
import com.kuzay2023.client.module.impl.HideScoreboardModule;
import com.kuzay2023.client.module.impl.InterfaceModule;
import com.kuzay2023.client.module.impl.InvisEspModule;
import com.kuzay2023.client.module.impl.ItemPhysicsModule;
import com.kuzay2023.client.module.impl.KeyClearChatModule;
import com.kuzay2023.client.module.impl.MediaRankModule;
import com.kuzay2023.client.module.impl.NametagsModule;
import com.kuzay2023.client.module.impl.OverlayModule;
import com.kuzay2023.client.module.impl.PlayerDetectionModule;
import com.kuzay2023.client.module.impl.RainNotiModule;
import com.kuzay2023.client.module.impl.FakeScoreboardModule;
import com.kuzay2023.client.module.impl.SetHomeHiddenModule;
import com.kuzay2023.client.module.impl.SimplePlaceholderModule;
import com.kuzay2023.client.module.impl.StorageEspModule;
import com.kuzay2023.client.module.impl.TabDetectorModule;
import com.kuzay2023.client.module.impl.VisualUsernameModule;
import com.kuzay2023.client.module.impl.ZoomModule;
import com.kuzay2023.client.module.impl.macro.KeyboardMacrosModule;
import com.kuzay2023.client.module.impl.gamble.GambleHelperModule;
import com.kuzay2023.client.module.impl.gamble.GamblesListerModule;
import com.kuzay2023.client.module.impl.gamble.HighestGamblersModule;
import com.kuzay2023.client.module.impl.gamble.RigModModule;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.money.AhSellModule;
import com.kuzay2023.client.module.impl.money.BlazeRodSniperModule;
import com.kuzay2023.client.module.impl.money.CrateBuyerModule;
import com.kuzay2023.client.module.impl.money.OrderDropperModule;
import com.kuzay2023.client.module.impl.money.OrderSniperModule;
import com.kuzay2023.client.module.impl.money.ShopBuyerModule;
import com.kuzay2023.client.module.impl.money.ShulkerDropperModule;
import com.kuzay2023.client.module.impl.money.TotemOrderModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class KuzayClientModClient implements ClientModInitializer {
	private static KuzayClientContext context;
	private static boolean openGuiKeyHeld;
	private static boolean panicKeyHeld;
	private static boolean rigSwitchKeyHeld;

	@Override
	public void onInitializeClient() {
		ConfigManager configManager = new ConfigManager();
		ModuleManager moduleManager = new ModuleManager();
		HudManager hudManager = new HudManager(configManager);
		ProfileManager profileManager = new ProfileManager();
		KeyBinding openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.kuzay2023sclient.open_gui",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_F9,
			"category.kuzay2023sclient.client"
		));
		KeyBinding panicKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.kuzay2023sclient.panic_stop",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"category.kuzay2023sclient.client"
		));
		KeyBinding rigSwitchSidesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.kuzay2023sclient.rig_switch_sides",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			"category.kuzay2023sclient.client"
		));

		context = new KuzayClientContext(configManager, moduleManager, hudManager, profileManager, openGuiKey, panicKey, rigSwitchSidesKey);
		registerBuiltIns(moduleManager, hudManager);
		applySafetyLabels(moduleManager);
		FakeScoreboardClientHooks.initialize();
		BalanceHooks.initialize();
		EspRenderManager.initialize();
		AutoReconnectScreenHooks.initialize();
		registerGambleHooks();
		configManager.load(context);
		VipAccessManager.initialize(context);
		applyCuratedTabLayout(context);
		configManager.save(context);
		profileManager.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client, context));
		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> hudManager.render(drawContext));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.getCurrentServerEntry() != null) {
				AutoReconnectManager.rememberLastServer(client.getCurrentServerEntry());
				String serverKey = ServerTabManager.currentServerKey(client);
				String serverLabel = ServerTabManager.currentServerLabel(client);
				if (!serverKey.isBlank()) {
					context.configManager().getConfig().knownServers.put(serverKey, serverLabel.isBlank() ? serverKey : serverLabel);
					if (context.configManager().getConfig().activeServerKey == null || context.configManager().getConfig().activeServerKey.isBlank()) {
						context.configManager().getConfig().activeServerKey = serverKey;
					}
					context.configManager().save(context);
				}
				context.profileManager().autoLoadForCurrentServer(context);
			}
			BalanceHooks.requestSilentRealBalance(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AutoReconnectManager.onDisconnected());

		KuzayClientMod.LOGGER.info("{} client bootstrap complete", KuzayClientMod.DISPLAY_NAME);
	}

	private static void registerBuiltIns(ModuleManager moduleManager, HudManager hudManager) {
		moduleManager.registerCategory("Gambles");
		moduleManager.registerCategory("Money");
		moduleManager.registerCategory("Other");
		moduleManager.registerCategory("Macros");
		moduleManager.registerCategory("VIP");
		moduleManager.registerCategory("Beta");

		moduleManager.register(new GamblesListerModule());
		moduleManager.register(new HighestGamblersModule());
		moduleManager.register(new GambleHelperModule());
		moduleManager.register(new SetHomeHiddenModule());
		moduleManager.register(new AutoCraterSlotsModule());
		moduleManager.register(new FreeLookModule());
		moduleManager.register(new FreecamModule());
		moduleManager.register(new MediaRankModule());
		moduleManager.register(new VisualUsernameModule());
		moduleManager.register(new FakeBalanceModule());
		moduleManager.register(new KeyClearChatModule());
		moduleManager.register(new RigModModule());
		moduleManager.register(new InterfaceModule());
		moduleManager.register(new OverlayModule(hudManager));
		moduleManager.register(new KeyboardMacrosModule());
		moduleManager.register(new ShulkerDropperModule());
		moduleManager.register(new ShopBuyerModule());
		moduleManager.register(new OrderSniperModule());
		moduleManager.register(new OrderDropperModule());
		moduleManager.register(new CrateBuyerModule());
		moduleManager.register(new BlazeRodSniperModule());
		moduleManager.register(new TotemOrderModule());
		moduleManager.register(new AhSellModule());
		moduleManager.register(new FakeScoreboardModule());
		moduleManager.register(new CoordSnapperModule());
		moduleManager.register(new HideScoreboardModule());
		moduleManager.register(new PlayerDetectionModule());
		moduleManager.register(new RainNotiModule());
		moduleManager.register(new TabDetectorModule());
		moduleManager.register(new FullbrightModule());
		moduleManager.register(new ZoomModule());
		moduleManager.register(new EspModule());
		moduleManager.register(new StorageEspModule());
		moduleManager.register(new InvisEspModule());
		moduleManager.register(new BlockEspModule());
		moduleManager.register(new NametagsModule());
		moduleManager.register(new AutoReconnectModule());
		registerListedPlaceholderModules(moduleManager);
	}

	private static void applyCuratedTabLayout(KuzayClientContext context) {
		Map<String, Set<String>> desiredTabs = new LinkedHashMap<>();
		addTabMembers(desiredTabs, "Gambles", "highest_gamblers", "gambles_lister");
		addTabMembers(desiredTabs, "Money",
			"ah_sell", "auto_order", "auto_sell", "auto_shell_order", "auto_shulker_order",
			"blaze_rod_dropper", "order_dropper", "shulker_dropper", "spawner_dropper"
		);
		addTabMembers(desiredTabs, "Other",
			"admin_list", "auto_reconnect", "gamble_helper", "crate_buyer", "emergency_seller",
			"free_look", "fullbright", "hide_scoreboard", "home_reset", "interface", "item_physics",
			"key_clear_chat", "media_rank_preview", "nametags", "no_block_interact", "overlay", "player_detection",
			"rain_noti", "shop_buyer", "spawner_protect", "storage_stealer", "tab_detector", "tpa_all_macro",
			"tpa_macro", "ui_helper", "visual_username", "zoom"
		);
		addTabMembers(desiredTabs, "Macros", "keyboard_macros");
		Set<String> vipOnly = new LinkedHashSet<>(Set.of(
			"ah_sniper", "auto_crater_slots", "auto_shulker_shell_order", "auto_spawner_sell", "blaze_rod_sniper",
			"block_esp", "coord_snapper", "esp", "fake_balance", "fake_scoreboard", "freecam", "invis_esp",
			"order_sniper", "rig_mod", "rtp_end_base_finder", "rtp_nether_base_finder", "rtper", "sethome_hidden",
			"spawner_order", "storage_esp", "totem_order"
		));

		for (Module module : context.moduleManager().getModules()) {
			module.clearExtraCategories();
			module.setOriginalCategoryHidden(false);
			module.setVipOnly(false);
			Set<String> tabs = desiredTabs.entrySet().stream()
				.filter(entry -> entry.getValue().contains(module.getId()))
				.map(Map.Entry::getKey)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

			if (vipOnly.contains(module.getId())) {
				tabs.clear();
				tabs.add("VIP");
				module.setVipOnly(true);
			}

			if (tabs.isEmpty()) {
				tabs.add(module.getOriginalCategory());
			}

			if (!tabs.contains(module.getOriginalCategory())) {
				module.setOriginalCategoryHidden(true);
			}

			for (String tab : tabs) {
				if (!tab.equals(module.getOriginalCategory())) {
					module.assignToTab(ServerTabManager.globalTab(tab));
				}
			}
		}
	}

	private static void addTabMembers(Map<String, Set<String>> desiredTabs, String tab, String... moduleIds) {
		desiredTabs.computeIfAbsent(tab, ignored -> new LinkedHashSet<>()).addAll(java.util.List.of(moduleIds));
	}

	private static void registerListedPlaceholderModules(ModuleManager moduleManager) {
		moduleManager.register(new SimplePlaceholderModule("ah_sniper", "Ah Sniper", "Snipes target items from auction listings.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("auto_order", "Auto Order", "Places repeating buy orders for the selected item.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("auto_sell", "Auto Sell", "Lists or sells the selected item automatically.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("auto_shulker_order", "Auto Shulker Order", "Automatically places shulker orders.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("auto_shulker_shell_order", "Auto Shulker Shell Order", "Automatically places shulker shell orders.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("blaze_rod_dropper", "Blaze Rod Dropper", "Drops blaze rods in configured batches.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("spawner_dropper", "Spawner Dropper", "Drops spawners in configured batches.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("spawner_order", "Spawner Order", "Places spawner orders with saved settings.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("admin_list", "Admin List", "Tracks and lists online staff members.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("emergency_seller", "Emergency Seller", "Emergency sale flow for a configured item.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("home_reset", "Home Reset", "Resets a saved home using the configured timing.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("no_block_interact", "No Block Interact", "Restricts accidental block interaction.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("rtp_end_base_finder", "Rtp End Base Finder", "Scans RTP routes for end base signs.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("rtp_nether_base_finder", "Rtp Nether Base Finder", "Scans RTP routes for nether bases.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("rtper", "Rtper", "Runs repeated RTP actions on a timer.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("spawner_protect", "Spawner Protect", "Protects nearby spawner placements.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("storage_stealer", "Storage Stealer", "Moves items out of storage using saved rules.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("tpa_all_macro", "Tpa All Macro", "Runs a multi-target TPA macro.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("tpa_macro", "Tpa Macro", "Runs a single-target TPA macro.", "Other"));
		moduleManager.register(new SimplePlaceholderModule("ui_helper", "Ui Helper", "Quality-of-life interface helpers.", "Other"));
		moduleManager.register(new ItemPhysicsModule());
		moduleManager.register(new SimplePlaceholderModule("auto_shell_order", "Auto Shell Order", "Automatically places shell orders.", "Money"));
		moduleManager.register(new SimplePlaceholderModule("auto_spawner_sell", "Auto Spawner Sell", "Automatically sells configured spawners.", "VIP"));
	}

	private static void registerGambleHooks() {
		GambleListerService.initialize();
		HighestGamblersService.initialize();

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			String text = message.getString();
			GambleListerService.handleChatMessage(text);
			HighestGamblersService.handleChatMessage(text);
		});

		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String text = message.getString();
			GambleListerService.handleChatMessage(text);
			HighestGamblersService.handleChatMessage(text);
		});

		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !SetHomeHiddenModule.shouldSuppressMessage(message.getString()));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> !SetHomeHiddenModule.shouldSuppressMessage(message.getString()));

		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : GambleHelperService.decoratePaymentMessage(message));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			Text updatedMessage = GambleHelperService.decoratePaymentMessage(message);
			if (updatedMessage == message) {
				return true;
			}

			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) {
				client.inGameHud.getChatHud().addMessage(updatedMessage);
			}
			return false;
		});
	}

	private static void applySafetyLabels(ModuleManager moduleManager) {
		Map<String, Module.SafetyLevel> safetyByCategoryAndName = Map.ofEntries(
			Map.entry(safetyKey("Gambles", "Gambles Lister"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Gambles", "Highest Gamblers"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Gambles", "Gamble Helper"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Gambles", "Rig Mod"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Shop Buyer"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Shulker Dropper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Order Sniper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Order Dropper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Crate Buyer"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Money", "Blaze Rod Sniper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Totem Snipper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Ah Sell"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Ah Sniper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Auto Order"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Auto Sell"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Auto Shulker Order"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Auto Shulker Shell Order"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Blaze Rod Dropper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Spawner Dropper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Money", "Spawner Order"), Module.SafetyLevel.BANNABLE),
			Map.entry(safetyKey("Other", "Set Home Hidden"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Auto Crater Slots"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Free Look"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Freecam"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Media Rank Preview"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Visual Username"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Fake Balance"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Key Clear Chat"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Interface"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Overlay"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Fake Scoreboard"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Admin List"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Coord Snapper"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Crate Buyer"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Emergency Seller"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Hide Scoreboard"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Home Reset"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "No Block Interact"), Module.SafetyLevel.BANNABLE),
			Map.entry(safetyKey("Other", "Player Detection"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Rain Noti"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Rtp End Base Finder"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Rtp Nether Base Finder"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Rtper"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Spawner Protect"), Module.SafetyLevel.BANNABLE),
			Map.entry(safetyKey("Other", "Storage Stealer"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Tab Detector"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Tpa All Macro"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Tpa Macro"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Ui Helper"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Fullbright"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Item Physics"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Macros", "Keybaord Macros"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "ESP"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Storage ESP"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Invis ESP"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Block ESP"), Module.SafetyLevel.RISKY),
			Map.entry(safetyKey("Other", "Nametags"), Module.SafetyLevel.SAFE),
			Map.entry(safetyKey("Other", "Auto Reconnect"), Module.SafetyLevel.SAFE)
		);

		for (Module module : moduleManager.getModules()) {
			module.setSafetyLevel(safetyByCategoryAndName.getOrDefault(safetyKey(module.getCategory(), module.getName()), Module.SafetyLevel.NONE));
		}
	}

	private static String safetyKey(String category, String name) {
		return normalizeSafetyPart(category) + "::" + normalizeSafetyPart(name);
	}

	private static String normalizeSafetyPart(String value) {
		if (value == null) {
			return "";
		}

		StringBuilder normalized = new StringBuilder();
		for (char character : value.toLowerCase(Locale.ROOT).toCharArray()) {
			if (Character.isLetterOrDigit(character)) {
				normalized.append(character);
			}
		}
		return normalized.toString();
	}

	private static void tick(MinecraftClient client, KuzayClientContext context) {
		applySafetyLabels(context.moduleManager());
		if (consumeGlobalKey(client, context.openGuiKey(), true, GlobalKey.OPEN_GUI)) {
			client.setScreen(new ClientScreen(context, Text.literal(KuzayClientMod.DISPLAY_NAME)));
		}
		if (client.player == null) {
			return;
		}
		if (consumeGlobalKey(client, context.panicKey(), false, GlobalKey.PANIC)) {
			disableAllModules(context, client);
		}
		if (consumeGlobalKey(client, context.rigSwitchSidesKey(), false, GlobalKey.RIG_SWITCH)) {
			switchRigSides(context);
		}

		BalanceHooks.tick(client);
		VipAccessManager.tick(client, context);
		AutoReconnectManager.tick(client);
		context.moduleManager().tick(client);
	}

	private static boolean consumeGlobalKey(MinecraftClient client, KeyBinding keyBinding, boolean allowWithoutPlayer, GlobalKey globalKey) {
		InputUtil.Key boundKey = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
		if (boundKey == InputUtil.UNKNOWN_KEY) {
			setHeld(globalKey, false);
			return false;
		}

		boolean pressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), boundKey.getCode());
		if (!allowWithoutPlayer && client.player == null) {
			pressed = false;
		}
		if (client.currentScreen != null && !isCtrlDown(client)) {
			pressed = false;
		}

		boolean triggered = pressed && !isHeld(globalKey);
		setHeld(globalKey, pressed);
		return triggered;
	}

	private static boolean isCtrlDown(MinecraftClient client) {
		long windowHandle = client.getWindow().getHandle();
		return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	private static boolean isHeld(GlobalKey globalKey) {
		return switch (globalKey) {
			case OPEN_GUI -> openGuiKeyHeld;
			case PANIC -> panicKeyHeld;
			case RIG_SWITCH -> rigSwitchKeyHeld;
		};
	}

	private static void setHeld(GlobalKey globalKey, boolean held) {
		switch (globalKey) {
			case OPEN_GUI -> openGuiKeyHeld = held;
			case PANIC -> panicKeyHeld = held;
			case RIG_SWITCH -> rigSwitchKeyHeld = held;
		}
	}

	private enum GlobalKey {
		OPEN_GUI,
		PANIC,
		RIG_SWITCH
	}

	private static void disableAllModules(KuzayClientContext context, MinecraftClient client) {
		for (Module module : context.moduleManager().getModules()) {
			if (module.isEnabled()) {
				module.setEnabled(false);
			}
		}
		if (client.player != null && client.currentScreen != null) {
			client.player.closeHandledScreen();
		}
		context.configManager().save(context);
	}

	private static void switchRigSides(KuzayClientContext context) {
		Module module = context.moduleManager().getModule("rig_mod");
		if (module instanceof RigModModule rigModModule) {
			rigModModule.switchSides();
			context.configManager().save(context);
		}
	}

	public static KuzayClientContext getContext() {
		return context;
	}

	public static void openHudEditor() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(new HudEditorScreen(context));
		}
	}
}
