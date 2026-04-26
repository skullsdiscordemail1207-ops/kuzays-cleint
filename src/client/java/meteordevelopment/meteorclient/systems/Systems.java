package meteordevelopment.meteorclient.systems;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Systems {
	private static final Map<Class<? extends System<?>>, System<?>> systems = new LinkedHashMap<>();
	private static final List<Runnable> preLoadTasks = new ArrayList<>(1);

	public static void addPreLoadTask(Runnable task) {
		preLoadTasks.add(task);
	}

	public static void init() {
		add(new Modules());

		Config config = (Config) add(new Config());
		registerColorSettings(config.settings, null);

		add(new Macros());
		add(new Friends());
	}

	public static System<?> add(System<?> system) {
		systems.put(castKey(system.getClass()), system);
		MeteorClient.EVENT_BUS.subscribe(system);
		system.init();
		return system;
	}

	public static void save(File folder) {
		long start = java.lang.System.currentTimeMillis();
		MeteorClient.LOG.info("Saving");

		for (System<?> system : values()) {
			system.save(folder);
		}

		MeteorClient.LOG.info("Saved in {} milliseconds.", java.lang.System.currentTimeMillis() - start);
	}

	public static void save() {
		save(null);
	}

	public static void load(File folder) {
		long start = java.lang.System.currentTimeMillis();
		MeteorClient.LOG.info("Loading");

		for (Runnable task : preLoadTasks) {
			task.run();
		}

		for (System<?> system : values()) {
			system.load(folder);
		}

		MeteorClient.LOG.info("Loaded in {} milliseconds", java.lang.System.currentTimeMillis() - start);
	}

	public static void load() {
		load(null);
	}

	public static <T extends System<?>> T get(Class<T> klass) {
		return klass.cast(systems.get(klass));
	}

	private static Collection<System<?>> values() {
		return systems.values();
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends System<?>> castKey(Class<?> klass) {
		return (Class<? extends System<?>>) klass;
	}

	private static void registerColorSettings(Settings settings, Module module) {
		if (settings != null) {
			settings.registerColorSettings(module);
		}
	}
}
