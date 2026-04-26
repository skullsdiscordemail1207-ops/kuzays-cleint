package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.ZoomModule;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererZoomMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void kuzayClient$applyZoomFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return;
		}

		Module module = context.moduleManager().getModule("zoom");
		if (module instanceof ZoomModule zoomModule && zoomModule.isEnabled()) {
			cir.setReturnValue((float) zoomModule.getActiveZoomFov());
		}
	}
}
