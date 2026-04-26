package com.kuzay2023.client.mixin.client;

import com.kuzay2023.client.gamble.RigService;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class RigItemStackMixin {
	@Shadow
	public abstract Item getItem();

	@Inject(method = "getName", at = @At("HEAD"), cancellable = true)
	private void kuzayClient$overrideRiggedName(CallbackInfoReturnable<Text> cir) {
		Item item = getItem();
		if (!RigService.isRigEnabled() || !RigService.isRiggedCandle(item) || !RigService.hasActiveNames()) {
			return;
		}

		int number = RigService.getDisplayedNumber((ItemStack) (Object) this);
		cir.setReturnValue(Text.literal(Integer.toString(number)));
	}
}
