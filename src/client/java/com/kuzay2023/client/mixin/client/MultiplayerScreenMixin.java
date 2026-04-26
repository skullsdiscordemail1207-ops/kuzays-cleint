package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.gui.AccountManagerScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
	protected MultiplayerScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void kuzay$addAccountButton(CallbackInfo ci) {
		addDrawableChild(ButtonWidget.builder(Text.literal("Account"), button -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) {
				client.setScreen(new AccountManagerScreen((Screen) (Object) this));
			}
		}).dimensions(this.width - 88, 8, 80, 20).build());
	}
}
