package meteordevelopment.meteorclient;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import com.kuzay2023.client.KuzayClientMod;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.ReflectInit;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class MeteorClient implements ClientModInitializer {
	public static final String MOD_ID = KuzayClientMod.MOD_ID;
	public static final ModMetadata MOD_META = FabricLoader.getInstance().getModContainer(KuzayClientMod.MOD_ID).orElseThrow().getMetadata();
	public static final String NAME = KuzayClientMod.DISPLAY_NAME;
	public static final Version VERSION = new Version(MOD_META.getVersion().getFriendlyString().split("-")[0]);
	public static final String BUILD_NUMBER = "kuzay2023s-client";
	public static MeteorClient INSTANCE = new MeteorClient();
	public static MeteorAddon ADDON;
	public static MinecraftClient mc;
	public static final IEventBus EVENT_BUS = new EventBus();
	public static final File FOLDER = FabricLoader.getInstance().getGameDir().resolve("kuzays-secrets-runtime").toFile();
	public static final Logger LOG = LoggerFactory.getLogger("kuzays-secrets-runtime");

	private static boolean initialized;

	@Override
	public void onInitializeClient() {
		if (initialized) {
			return;
		}

		mc = MinecraftClient.getInstance();
		// Always initialize even during splash screen to prevent mixin crashes
		// if (!isClientReady()) {
		// 	return;
		// }

		initialized = true;
		INSTANCE = this;
		if (!FOLDER.exists()) {
			FOLDER.mkdirs();
		}

		LOG.info("Initializing secret runtime for {}", NAME);
		MeteorExecutor.init();
		AddonManager.init();
		registerLambdaFactories();
		PostProcessShaders.init();

		ReflectInit.registerPackages();
		ReflectInit.init(PreInit.class);
		Categories.init();
		Systems.init();
		EVENT_BUS.subscribe(this);

		for (MeteorAddon addon : AddonManager.ADDONS) {
			addon.onInitialize();
		}

		Modules.get().sortModules();
		Systems.load();
		ReflectInit.init(PostInit.class);
		Runtime.getRuntime().addShutdownHook(new Thread(Systems::save));
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}

	private static void registerAddon(MeteorAddon addon, String name, String... authors) {
		addon.name = name;
		addon.authors = authors;
		AddonManager.ADDONS.add(addon);
	}

	private static void registerLambdaFactories() {
		EVENT_BUS.registerLambdaFactory("meteordevelopment.meteorclient", (method, klass) -> MethodHandles.privateLookupIn(klass, MethodHandles.lookup()));
	}

	private static boolean isClientReady() {
		if (mc == null || mc.getWindow() == null) {
			return false;
		}

		try {
			return GLFW.glfwGetCurrentContext() != 0L && GL.getCapabilities() != null;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
