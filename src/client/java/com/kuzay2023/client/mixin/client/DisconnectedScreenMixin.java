package com.kuzay2023.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.network.AutoReconnectManager;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
	@Unique
	private ButtonWidget kuzayClient$autoReconnectButton;

	protected DisconnectedScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void kuzayClient$addAutoReconnectButton(CallbackInfo ci) {
		AutoReconnectManager.scheduleReconnect();
		kuzayClient$autoReconnectButton = addDrawableChild(ButtonWidget.builder(Text.literal(buttonLabel()), button -> {
			AutoReconnectManager.setAutoReconnectEnabled(!AutoReconnectManager.isAutoReconnectEnabled());
			if (AutoReconnectManager.isAutoReconnectEnabled()) {
				AutoReconnectManager.scheduleReconnect();
			} else {
				AutoReconnectManager.cancelReconnect();
			}
			button.setMessage(Text.literal(buttonLabel()));
		}).dimensions(this.width / 2 - 100, this.height - 52, 200, 20).build());
	}

	@Unique
	private String buttonLabel() {
		if (!AutoReconnectManager.isAutoReconnectEnabled()) {
			return "Auto Reconnect: OFF";
		}
		int seconds = AutoReconnectManager.getSecondsRemaining();
		return seconds >= 0 ? "Auto Reconnect: ON (" + seconds + "s)" : "Auto Reconnect: ON";
	}
}
