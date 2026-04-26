package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.kuzay2023.client.player.PlayerNameDecorator;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
	@Shadow
	public abstract GameProfile getProfile();

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void kuzayClient$decoratePlayerListName(CallbackInfoReturnable<Text> cir) {
		cir.setReturnValue(PlayerNameDecorator.decoratePlayerListName(getProfile(), cir.getReturnValue()));
	}
}
