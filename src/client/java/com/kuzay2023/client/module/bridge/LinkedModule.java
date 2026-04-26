package com.kuzay2023.client.module.bridge;

import org.lwjgl.glfw.GLFW;

import com.kuzay2023.client.module.Module;

import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.client.MinecraftClient;

public class LinkedModule extends Module {
	private final meteordevelopment.meteorclient.systems.modules.Module runtimeModule;

	public LinkedModule(meteordevelopment.meteorclient.systems.modules.Module runtimeModule, String category) {
		super("secret_" + runtimeModule.name, runtimeModule.title, runtimeModule.description, category);
		this.runtimeModule = runtimeModule;
	}

	public meteordevelopment.meteorclient.systems.modules.Module getRuntimeModule() {
		return runtimeModule;
	}

	@Override
	public void toggle() {
		runtimeModule.toggle();
		Systems.save();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (runtimeModule.isActive() != enabled) {
			runtimeModule.toggle();
			Systems.save();
		}
	}

	@Override
	public void setEnabledSilently(boolean enabled) {
		setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return runtimeModule.isActive();
	}

	@Override
	public int getBoundKey() {
		if (!runtimeModule.keybind.isSet()) {
			return GLFW.GLFW_KEY_UNKNOWN;
		}
		if (!runtimeModule.keybind.isKey()) {
			return Module.encodeMouseButton(runtimeModule.keybind.getValue());
		}
		return runtimeModule.keybind.getValue();
	}

	@Override
	public void setBoundKey(int boundKey) {
		if (boundKey == GLFW.GLFW_KEY_UNKNOWN) {
			runtimeModule.keybind.reset();
		} else if (Module.isMouseBinding(boundKey)) {
			runtimeModule.keybind.set(false, Module.getMouseButton(boundKey), 0);
		} else {
			runtimeModule.keybind.set(true, boundKey, 0);
		}
		Systems.save();
	}

	@Override
	public void setBoundKeySilently(int boundKey) {
		setBoundKey(boundKey);
	}

	@Override
	public String getBoundKeyName() {
		return Module.describeBoundKey(getBoundKey());
	}

	@Override
	public void updateKeybind(MinecraftClient client) {
		// The bundled runtime manages this module's keybind handling itself.
	}
}
