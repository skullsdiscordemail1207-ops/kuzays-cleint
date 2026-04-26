package com.kuzay2023.client.hud;

import com.kuzay2023.client.gamble.GambleListerService;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;

public class GambleListerHudElement extends HudElement {
	public GambleListerHudElement() {
		super("gamble_lister", "Payment Lister", 20.0F, 20.0F);
	}

	@Override
	public int getWidth() {
		return GambleListerService.getScaledWidth();
	}

	@Override
	public int getHeight() {
		return GambleListerService.getScaledHeight();
	}

	@Override
	public void render(DrawContext context, float globalScale) {
		if (!GambleListerService.isEnabled()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		var hudState = GambleListerService.getHudState();
		boolean editing = client.currentScreen instanceof ChatScreen;
		int baseWidth = GambleListerService.getBaseWidth();
		int baseHeight = GambleListerService.getBaseHeight();

		context.getMatrices().push();
		context.getMatrices().scale((float) hudState.scale, (float) hudState.scale, 1.0F);

		context.fill(0, 0, baseWidth, baseHeight, editing ? 0xAA10131A : 0x9010131A);
		context.fill(0, 0, baseWidth, 14, 0xD0511414);
		context.drawText(client.textRenderer, Text.literal("Payment lister"), 5, 3, 0xFFFFFF, false);

		if (GambleListerService.entries().isEmpty()) {
			context.drawText(client.textRenderer, Text.literal("No payments yet"), 5, 19, 0xBFC7BF, false);
		} else {
			for (int index = 0; index < GambleListerService.entries().size(); index++) {
				var entry = GambleListerService.entries().get(index);
				int rowY = 18 + (index * 12);
				context.drawText(client.textRenderer, Text.literal((index + 1) + ". " + entry.payerName()), 5, rowY, 0xFFFFFF, false);
				Text amount = Text.literal("$" + entry.amountText());
				int amountX = baseWidth - 6 - client.textRenderer.getWidth(amount);
				context.drawText(client.textRenderer, amount, amountX, rowY, 0x55FF55, false);
			}
		}

		int footerTop = baseHeight - 16;
		context.fill(0, footerTop, baseWidth, baseHeight, 0x70262A33);

		int removeButtonWidth = 56;
		int setKeyButtonWidth = 48;
		int buttonHeight = 12;
		int buttonY = baseHeight - buttonHeight - 2;
		int removeButtonX = baseWidth - removeButtonWidth - 6;
		int setKeyButtonX = removeButtonX - setKeyButtonWidth - 4;

		context.fill(setKeyButtonX, buttonY, setKeyButtonX + setKeyButtonWidth, buttonY + buttonHeight, GambleListerService.isListeningForRemoveKey() ? 0xD09A6A12 : 0x90545A68);
		context.fill(setKeyButtonX, buttonY, setKeyButtonX + setKeyButtonWidth, buttonY + 1, 0xFFFFFFFF);
		context.fill(setKeyButtonX, buttonY + buttonHeight - 1, setKeyButtonX + setKeyButtonWidth, buttonY + buttonHeight, 0xFFFFFFFF);
		context.fill(setKeyButtonX, buttonY, setKeyButtonX + 1, buttonY + buttonHeight, 0xFFFFFFFF);
		context.fill(setKeyButtonX + setKeyButtonWidth - 1, buttonY, setKeyButtonX + setKeyButtonWidth, buttonY + buttonHeight, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(GambleListerService.isListeningForRemoveKey() ? "Press key" : "Set Key"), setKeyButtonX + (setKeyButtonWidth / 2), buttonY + 2, 0xFFFFFF);

		context.fill(removeButtonX, buttonY, removeButtonX + removeButtonWidth, buttonY + buttonHeight, editing ? 0xD08F2424 : 0x904A4A4A);
		context.fill(removeButtonX, buttonY, removeButtonX + removeButtonWidth, buttonY + 1, 0xFFFFFFFF);
		context.fill(removeButtonX, buttonY + buttonHeight - 1, removeButtonX + removeButtonWidth, buttonY + buttonHeight, 0xFFFFFFFF);
		context.fill(removeButtonX, buttonY, removeButtonX + 1, buttonY + buttonHeight, 0xFFFFFFFF);
		context.fill(removeButtonX + removeButtonWidth - 1, buttonY, removeButtonX + removeButtonWidth, buttonY + buttonHeight, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("Remove Top"), removeButtonX + (removeButtonWidth / 2), buttonY + 2, 0xFFFFFF);

		if (GambleListerService.isListeningForRemoveKey()) {
			context.drawText(client.textRenderer, Text.literal("Press a key or Esc"), 5, footerTop + 4, 0xF0D7A4, false);
		} else if (GambleListerService.getRemoveTopKey() != null) {
			context.drawText(client.textRenderer, Text.literal("Key: ").append(GambleListerService.getRemoveTopKey().getBoundKeyLocalizedText()), 5, footerTop + 4, 0xD8DDE8, false);
		}

		if (editing) {
			int handleSize = 12;
			int handleX = baseWidth - handleSize;
			int handleY = baseHeight - handleSize;
			context.fill(handleX, handleY, baseWidth, baseHeight, 0xD0E0E0E0);
			context.drawText(client.textRenderer, Text.literal("+"), handleX + 3, handleY + 2, 0x303030, false);
		}

		context.getMatrices().pop();
	}
}
