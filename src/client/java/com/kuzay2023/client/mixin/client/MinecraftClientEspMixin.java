package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kuzay2023.client.render.EspRenderManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientEspMixin {
	@Inject(method = "hasOutline(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void kuzayClient$forceEspOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (EspRenderManager.shouldOutlineEntity(entity)) {
			cir.setReturnValue(true);
		}
	}
}
