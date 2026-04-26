package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.ZoomModule;

import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public abstract class ZoomMouseMixin {
	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void kuzayClient$handleZoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return;
		}

		Module module = context.moduleManager().getModule("zoom");
		if (module instanceof ZoomModule zoomModule && zoomModule.isEnabled() && zoomModule.handleScroll(vertical)) {
			ci.cancel();
		}
	}
}
