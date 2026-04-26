package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.kuzay2023.client.freecam.FreecamController;
import com.kuzay2023.client.freelook.FreeLookController;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;

@Mixin(Mouse.class)
public abstract class FreeLookMouseMixin {
	@Redirect(
		method = "updateMouse",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
		)
	)
	private void kuzayClient$redirectLookDirection(ClientPlayerEntity player, double deltaX, double deltaY) {
		if (FreecamController.handleMouseDelta(MinecraftClient.getInstance(), deltaX, deltaY)) {
			return;
		}
		if (!FreeLookController.handleMouseDelta(player, deltaX, deltaY)) {
			player.changeLookDirection(deltaX, deltaY);
		}
	}
}
