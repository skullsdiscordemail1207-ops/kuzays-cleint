package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.freelook.FreeLookController;
import com.kuzay2023.client.player.PlayerNameDecorator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityAttachmentType;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(
		method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
		at = @At("TAIL")
	)
	private void kuzayClient$forceFreeLookLabels(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null || !FreeLookController.isActive()) {
			return;
		}

		boolean isLocalPlayer = player.getUuid().equals(client.player.getUuid());
		boolean isTargetedPlayer = player.getId() == FreeLookController.getTargetedEntityId()
			|| (FreeLookController.getTargetedPlayerUuid() != null
				&& FreeLookController.getTargetedPlayerUuid().equals(player.getUuid()));
		if (!isLocalPlayer && !isTargetedPlayer) {
			return;
		}

		state.displayName = PlayerNameDecorator.decorateEntityDisplayName(player, player.getDisplayName());
		state.nameLabelPos = player.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, player.getLerpedYaw(tickDelta));
	}
}
