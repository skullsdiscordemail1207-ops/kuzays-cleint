package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

@Mixin(MinecraftClient.class)
public interface MinecraftClientSessionAccessor {
	@Mutable
	@Accessor("session")
	void kuzay$setSession(Session session);
}
