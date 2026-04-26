package com.kuzay2023.client.mixin.client;

import com.kuzay2023.client.gamble.RigService;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class RigHandledScreenMixin {
	@Unique
	private static final Identifier KUZAY_CLIENT_DISPENSER_TEXTURE = Identifier.ofVanilla("textures/gui/container/dispenser.png");

	@Shadow
	protected ScreenHandler handler;

	@Shadow
	protected int backgroundWidth;

	@Unique
	private static final int KUZAY_CLIENT_CONTAINER_SLOT_COUNT = 9;

	@Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
	private void kuzayClient$drawFakeEmptySlot(DrawContext context, Slot slot, CallbackInfo ci) {
		if (!kuzayClient$shouldFakeContainerSlot(slot)) {
			return;
		}

		context.drawTexture(id -> net.minecraft.client.render.RenderLayer.getGuiTextured(id), KUZAY_CLIENT_DISPENSER_TEXTURE, slot.x, slot.y, (float) slot.x, (float) slot.y, 16, 16, 256, 256);
		ci.cancel();
	}

	@Inject(method = "drawSlots", at = @At("TAIL"))
	private void kuzayClient$adjustVisualGap(DrawContext context, CallbackInfo ci) {
		if (!RigService.isRigEnabled() || !RigService.hasActiveNames() || !(handler instanceof Generic3x3ContainerScreenHandler screenHandler)) {
			return;
		}

		int actualEmptySlot = -1;
		Item matchedItem = null;
		ItemStack displayStack = ItemStack.EMPTY;

		for (int slotIndex = 0; slotIndex < KUZAY_CLIENT_CONTAINER_SLOT_COUNT; slotIndex++) {
			Slot slot = screenHandler.getSlot(slotIndex);
			ItemStack stack = slot.getStack();

			if (stack.isEmpty()) {
				if (actualEmptySlot != -1) return;
				actualEmptySlot = slotIndex;
				continue;
			}

			Item riggedItem = kuzayClient$resolveRiggedItem(stack);
			if (riggedItem == null) return;

			if (matchedItem == null) {
				matchedItem = riggedItem;
				displayStack = stack;
			} else if (matchedItem != riggedItem) {
				return;
			}
		}

		int desiredEmptySlot = kuzayClient$getDesiredEmptySlot(screenHandler);
		if (actualEmptySlot == -1 || desiredEmptySlot == -1 || matchedItem == null || displayStack.isEmpty() || actualEmptySlot == desiredEmptySlot) {
			return;
		}

		Slot actualSlot = screenHandler.getSlot(actualEmptySlot);
		int seed = actualSlot.x + actualSlot.y * backgroundWidth;
		context.drawItem(displayStack, actualSlot.x, actualSlot.y, seed);
		context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, displayStack, actualSlot.x, actualSlot.y, null);
	}

	@Unique
	private static Item kuzayClient$resolveRiggedItem(ItemStack stack) {
		if (stack.isOf(RigService.getWinnerItem())) return RigService.getWinnerItem();
		if (stack.isOf(RigService.getLoserItem())) return RigService.getLoserItem();
		return null;
	}

	@Unique
	private boolean kuzayClient$shouldFakeContainerSlot(Slot slot) {
		if (!RigService.isRigEnabled() || !RigService.hasActiveNames() || !(handler instanceof Generic3x3ContainerScreenHandler screenHandler)) {
			return false;
		}

		int desiredEmptySlot = kuzayClient$getDesiredEmptySlot(screenHandler);
		return desiredEmptySlot != -1 && slot.id == desiredEmptySlot;
	}

	@Unique
	private int kuzayClient$getDesiredEmptySlot(Generic3x3ContainerScreenHandler screenHandler) {
		Item matchedItem = null;
		int emptyCount = 0;

		for (int slotIndex = 0; slotIndex < KUZAY_CLIENT_CONTAINER_SLOT_COUNT; slotIndex++) {
			ItemStack stack = screenHandler.getSlot(slotIndex).getStack();
			if (stack.isEmpty()) {
				emptyCount++;
				continue;
			}

			Item riggedItem = kuzayClient$resolveRiggedItem(stack);
			if (riggedItem == null) return -1;

			if (matchedItem == null) matchedItem = riggedItem;
			else if (matchedItem != riggedItem) return -1;
		}

		if (matchedItem == null || emptyCount != 1) return -1;
		int displayedNumber = RigService.getDisplayedNumber(new ItemStack(matchedItem));
		return Math.max(0, Math.min(KUZAY_CLIENT_CONTAINER_SLOT_COUNT - 1, displayedNumber - 1));
	}
}
