package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kuzay2023.client.freelook.FreeLookController;
import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.NametagsModule;
import com.kuzay2023.client.player.PlayerNameDecorator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "getDisplayName(Lnet/minecraft/entity/Entity;)Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
	private void kuzayClient$decoratePlayerDisplayName(Entity entity, CallbackInfoReturnable<Text> cir) {
		cir.setReturnValue(PlayerNameDecorator.decorateEntityDisplayName(entity, cir.getReturnValue()));
	}

	@Inject(method = "hasLabel(Lnet/minecraft/entity/Entity;D)Z", at = @At("RETURN"), cancellable = true)
	private void kuzayClient$showFreeLookLabels(Entity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null || !FreeLookController.isActive()) {
			return;
		}

		if (entity.getUuid().equals(client.player.getUuid())
			|| entity.getId() == FreeLookController.getTargetedEntityId()
			|| (FreeLookController.getTargetedPlayerUuid() != null
				&& FreeLookController.getTargetedPlayerUuid().equals(entity.getUuid()))) {
			cir.setReturnValue(true);
			return;
		}

		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return;
		}
		Module module = context.moduleManager().getModule("nametags");
		if (module instanceof NametagsModule nametagsModule && module.isEnabled() && nametagsModule.shouldShow(entity, client.player)) {
			cir.setReturnValue(true);
		}
	}
}
