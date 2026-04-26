package meteordevelopment.meteorclient.systems.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public class Modules extends System<Modules> {
	public static final ModuleRegistry REGISTRY = new ModuleRegistry();

	private static final List<Category> CATEGORIES = new ArrayList<>();
	private static final String[] RUNTIME_MODULE_CLASSES = {
		"meteordevelopment.meteorclient.systems.modules.world.Timer",
		"meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder",
		"meteordevelopment.meteorclient.systems.modules.player.LiquidInteract",
		"meteordevelopment.meteorclient.systems.modules.player.NoMiningTrace",
		"meteordevelopment.meteorclient.systems.modules.render.Freecam",
		"meteordevelopment.meteorclient.systems.modules.render.NoRender",
		"meteordevelopment.meteorclient.systems.modules.render.Zoom",
		"meteordevelopment.meteorclient.systems.modules.render.blockesp.BlockESP",
		"meteordevelopment.meteorclient.systems.modules.render.ESP",
		"meteordevelopment.meteorclient.systems.modules.render.Fullbright",
		"meteordevelopment.meteorclient.systems.modules.render.ItemPhysics",
		"meteordevelopment.meteorclient.systems.modules.render.Nametags"
	};

	private final List<Module> modules = new ArrayList<>();
	private final Map<Class<? extends Module>, Module> moduleInstances = new LinkedHashMap<>();
	private final Map<Category, List<Module>> groups = new LinkedHashMap<>();
	private final List<Module> active = new ArrayList<>();
	private Module moduleToBind;
	private boolean awaitingKeyRelease;

	public Modules() {
		super("modules");
	}

	public static Modules get() {
		return meteordevelopment.meteorclient.systems.Systems.get(Modules.class);
	}

	@Override
	public void init() {
		for (String className : RUNTIME_MODULE_CLASSES) {
			addByClassName(className);
		}
	}

	public static void registerCategory(Category category) {
		if (!Categories.REGISTERING) {
			throw new RuntimeException("Modules.registerCategory - Cannot register category outside of onRegisterCategories callback.");
		}

		CATEGORIES.add(category);
	}

	public static Iterable<Category> loopCategories() {
		return CATEGORIES;
	}

	public static Category getCategoryByHash(int hash) {
		for (Category category : CATEGORIES) {
			if (category.hashCode() == hash) {
				return category;
			}
		}

		return null;
	}

	public <T extends Module> T get(Class<T> klass) {
		return klass.cast(moduleInstances.get(klass));
	}

	public Module get(String name) {
		for (Module module : moduleInstances.values()) {
			if (module.name.equalsIgnoreCase(name)) {
				return module;
			}
		}

		return null;
	}

	public boolean isActive(Class<? extends Module> klass) {
		Module module = get(klass);
		return module != null && module.isActive();
	}

	public List<Module> getGroup(Category category) {
		return groups.computeIfAbsent(category, key -> new ArrayList<>());
	}

	public Collection<Module> getAll() {
		return moduleInstances.values();
	}

	public List<Module> getList() {
		return modules;
	}

	public int getCount() {
		return moduleInstances.size();
	}

	public List<Module> getActive() {
		synchronized (active) {
			return active;
		}
	}

	public Set<Module> searchTitles(String text) {
		String query = text == null ? "" : text.toLowerCase();
		Set<Module> results = new LinkedHashSet<>();

		for (Module module : moduleInstances.values()) {
			if (module.title.toLowerCase().contains(query)) {
				results.add(module);
			}
		}

		return results;
	}

	public Set<Module> searchSettingTitles(String text) {
		String query = text == null ? "" : text.toLowerCase();
		Set<Module> results = new LinkedHashSet<>();

		for (Module module : moduleInstances.values()) {
			for (SettingGroup group : module.settings) {
				for (Setting<?> setting : group) {
					String title = setting.title == null ? "" : setting.title.toLowerCase();
					if (title.contains(query)) {
						results.add(module);
						break;
					}
				}
			}
		}

		return results;
	}

	public void sortModules() {
		for (List<Module> group : groups.values()) {
			group.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
		}

		modules.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
	}

	public void add(Module module) {
		if (!CATEGORIES.contains(module.category)) {
			throw new RuntimeException("Modules.addModule - Module's category was not registered.");
		}

		Module existing = get(module.name);
		if (existing != null) {
			moduleInstances.remove(existing.getClass());
			modules.remove(existing);
			getGroup(existing.category).remove(existing);
		}

		moduleInstances.put(module.getClass(), module);
		modules.add(module);
		getGroup(module.category).add(module);
		module.settings.registerColorSettings(module);
	}

	public void removeActive(Module module) {
		synchronized (active) {
			active.remove(module);
		}
	}

	public void addActive(Module module) {
		synchronized (active) {
			if (!active.contains(module)) {
				active.add(module);
			}
		}
	}

	public void disableAll() {
		for (Module module : new ArrayList<>(moduleInstances.values())) {
			if (module.isActive()) {
				module.toggle();
			}
		}
	}

	public void setModuleToBind(Module module) {
		moduleToBind = module;
	}

	public void awaitKeyRelease() {
		awaitingKeyRelease = true;
	}

	public boolean isBinding() {
		return moduleToBind != null;
	}

	@EventHandler(priority = 200)
	private void onKeyBinding(KeyEvent event) {
		if (event.action == KeyAction.Release && onBinding(true, event.key, event.modifiers)) {
			event.cancel();
		}
	}

	@EventHandler(priority = 200)
	private void onButtonBinding(MouseButtonEvent event) {
		if (event.action == KeyAction.Release && onBinding(false, event.button, 0)) {
			event.cancel();
		}
	}

	private boolean onBinding(boolean isKey, int value, int modifiers) {
		if (!isBinding()) {
			return false;
		}

		if (awaitingKeyRelease) {
			if (isKey && value != 257 && value != 335) {
				return false;
			}

			awaitingKeyRelease = false;
			return false;
		}

		if (moduleToBind.keybind.canBindTo(isKey, value, modifiers)) {
			moduleToBind.keybind.set(isKey, value, modifiers);
			moduleToBind.info("Bound to (highlight)%s(default).", moduleToBind.keybind);
		}
		else if (value == 256) {
			moduleToBind.keybind.set(meteordevelopment.meteorclient.utils.misc.Keybind.none());
			moduleToBind.info("Removed bind.");
		}
		else {
			return false;
		}

		MeteorClient.EVENT_BUS.post(ModuleBindChangedEvent.get(moduleToBind));
		moduleToBind = null;
		return true;
	}

	@EventHandler
	private void onKey(KeyEvent event) {
		if (event.action == KeyAction.Repeat) {
			return;
		}

		onAction(true, event.key, event.modifiers, event.action == KeyAction.Press);
	}

	@EventHandler
	private void onMouseButton(MouseButtonEvent event) {
		if (event.action == KeyAction.Repeat) {
			return;
		}

		onAction(false, event.button, 0, event.action == KeyAction.Press);
	}

	private void onAction(boolean isKey, int value, int modifiers, boolean pressed) {
		if (MeteorClient.mc.currentScreen != null || Input.isKeyPressed(292)) {
			return;
		}

		for (Module module : moduleInstances.values()) {
			if (!module.keybind.matches(isKey, value, modifiers)) {
				continue;
			}

			if (!pressed && module.toggleOnBindRelease && module.isActive()) {
				module.toggle();
				module.sendToggledMsg();
			}
			else if (pressed) {
				module.toggle();
				module.sendToggledMsg();
			}
		}
	}

	@EventHandler
	private void onOpenScreen(OpenScreenEvent event) {
		if (!Utils.canUpdate()) {
			return;
		}

		for (Module module : moduleInstances.values()) {
			if (module.toggleOnBindRelease && module.isActive()) {
				module.toggle();
				module.sendToggledMsg();
			}
		}
	}

	@Override
	public NbtCompound toTag() {
		NbtCompound root = new NbtCompound();
		NbtList list = new NbtList();

		for (Module module : modules) {
			NbtCompound moduleTag = module.toTag();
			if (moduleTag != null) {
				list.add(moduleTag);
			}
		}

		root.put("modules", list);
		return root;
	}

	@Override
	public Modules fromTag(NbtCompound tag) {
		disableAll();

		NbtList list = tag.getList("modules", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound moduleTag = list.getCompound(i);
			Module module = get(moduleTag.getString("name"));
			if (module != null) {
				module.fromTag(moduleTag);
			}
		}

		return this;
	}

	private void addByClassName(String className) {
		try {
			Class<?> klass = Class.forName(className);
			if (!Module.class.isAssignableFrom(klass)) {
				return;
			}

			Module module = (Module) klass.getDeclaredConstructor().newInstance();
			add(module);
		}
		catch (Throwable throwable) {
			MeteorClient.LOG.warn("Skipping secret runtime module {}", className, throwable);
		}
	}

	public static class ModuleRegistry {
	}
}
