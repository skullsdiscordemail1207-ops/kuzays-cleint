package com.kuzay2023.client.module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.MinecraftClient;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final Set<String> categories = new LinkedHashSet<>();

	public void registerCategory(String category) {
		categories.add(category);
	}

	public void register(Module module) {
		modules.add(module);
		categories.add(module.getOriginalCategory());
		sortModules();
	}

	public List<Module> getModules() {
		return modules;
	}

	public List<String> getCategories() {
		return new ArrayList<>(categories);
	}

	public void sortModules() {
		modules.sort(Comparator.comparing(Module::getCategory).thenComparing(Module::getName));
	}

	public Module getModule(String id) {
		for (Module module : modules) {
			if (module.getId().equals(id)) {
				return module;
			}
		}
		return null;
	}

	public void tick(MinecraftClient client) {
		for (Module module : modules) {
			module.updateKeybind(client);
			module.tickScheduled(client);
			if (module.isEnabled()) {
				module.tick(client);
			}
		}
	}
}
