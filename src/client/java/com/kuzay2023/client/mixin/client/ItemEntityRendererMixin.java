package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.ItemPhysicsModule;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void kuzayClient$applyItemPhysics(ItemEntity itemEntity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return;
		}

		Module module = context.moduleManager().getModule("item_physics");
		if (!(module instanceof ItemPhysicsModule itemPhysicsModule) || !itemPhysicsModule.isEnabled()) {
			return;
		}
		if (itemPhysicsModule.onlyOnGround() && !itemEntity.isOnGround()) {
			return;
		}

		float rotation = (itemEntity.getItemAge() + tickDelta) * (6.0F * itemPhysicsModule.rotationSpeed());
		matrices.translate(0.0F, 0.02F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
	}
}
