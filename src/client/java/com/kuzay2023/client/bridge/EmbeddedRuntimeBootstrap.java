package com.kuzay2023.client.bridge;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import net.minecraft.client.MinecraftClient;

public final class EmbeddedRuntimeBootstrap {
	private static boolean initialized;
	private static boolean initializing;

	private EmbeddedRuntimeBootstrap() {
	}

	public static void ensureInitialized() {
		if (initialized || initializing || !isClientReady()) {
			return;
		}

		initializing = true;
		try {
			meteordevelopment.meteorclient.MeteorClient.INSTANCE.onInitializeClient();
			initialized = true;
		} finally {
			initializing = false;
		}
	}

	private static boolean isClientReady() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}

		try {
			return GLFW.glfwGetCurrentContext() != 0L && GL.getCapabilities() != null;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
