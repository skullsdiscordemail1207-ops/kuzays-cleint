package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.freecam.FreecamController;
import com.kuzay2023.client.freelook.FreeLookController;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;

@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
	@Shadow
	private float lastCameraY;

	@Shadow
	private float cameraY;

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void moveBy(float x, float y, float z);

	@Shadow
	protected abstract void setPos(double x, double y, double z);

	@Shadow
	protected abstract float clipToSpace(float desiredCameraDistance);

	@Inject(method = "update", at = @At("TAIL"))
	private void kuzayClient$applyFreeLookRotation(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (FreecamController.isActive()) {
			setRotation(FreecamController.getRenderYaw(tickDelta), FreecamController.getRenderPitch(tickDelta));
			setPos(FreecamController.getRenderPosition(tickDelta).x, FreecamController.getRenderPosition(tickDelta).y, FreecamController.getRenderPosition(tickDelta).z);
			return;
		}
		if (!FreeLookController.isActive() || client == null || client.player == null || focusedEntity != client.player || !thirdPerson) {
			return;
		}

		setRotation(FreeLookController.getCameraYaw(), FreeLookController.getCameraPitch());
		setPos(
			MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
			MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + MathHelper.lerp(tickDelta, lastCameraY, cameraY),
			MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
		);

		float scale = focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
		moveBy(-clipToSpace(4.0F * scale), 0.0F, 0.0F);
	}
}
