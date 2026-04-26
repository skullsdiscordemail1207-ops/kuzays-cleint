package com.kuzay2023.client.gui;

import java.util.function.Consumer;
import java.util.concurrent.CompletionException;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.gui.render.UiRenderer;
import com.kuzay2023.client.tab.VipAccessManager;
import com.kuzay2023.client.tab.VipUnlockResult;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class VipPasswordScreen extends Screen {
	private final Screen parent;
	private final Consumer<Boolean> resultConsumer;

	private TextFieldWidget passwordField;
	private String feedback = "";
	private int feedbackColor = 0xFFE34B4B;
	private boolean submitting;

	public VipPasswordScreen(Screen parent, Consumer<Boolean> resultConsumer) {
		super(Text.literal("VIP Access"));
		this.parent = parent;
		this.resultConsumer = resultConsumer;
	}

	@Override
	protected void init() {
		passwordField = new TextFieldWidget(textRenderer, width / 2 - 120, height / 2 - 10, 240, 20, Text.literal("VIP Password"));
		passwordField.setMaxLength(128);
		addDrawableChild(passwordField);
		setInitialFocus(passwordField);
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		drawContext.fill(0, 0, width, height, 0xE0101418);
		int left = width / 2 - 170;
		int top = height / 2 - 70;
		UiRenderer.drawPanel(drawContext, left, top, 340, 150, 0xEE1B2421, 0xFF324039);
		UiRenderer.drawText(drawContext, textRenderer, "VIP Tab", left + 18, top + 18, 0xFFF4F6F8);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Enter the VIP password to unlock premium modules.", left + 18, top + 36, 304, 0xB4F4F6F8);
		UiRenderer.drawPill(drawContext, left + 18, top + 90, 140, 22, 0xFF2EC27E);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Unlock", left + 66, top + 97, 44, 0xFF07131A);
		UiRenderer.drawPill(drawContext, left + 182, top + 90, 140, 22, 0xFF2A3530);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Cancel", left + 232, top + 97, 40, 0xFFF4F6F8);
		if (!feedback.isBlank()) {
			UiRenderer.drawTrimmedText(drawContext, textRenderer, feedback, left + 18, top + 120, 304, feedbackColor);
		}
		if (submitting) {
			UiRenderer.drawTrimmedText(drawContext, textRenderer, "Checking key...", left + 18, top + 120, 304, 0xFF8CD5FF);
		}
		super.render(drawContext, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int left = width / 2 - 170;
		int top = height / 2 - 70;
		if (UiRenderer.isHovered(mouseX, mouseY, left + 18, top + 90, 140, 22)) {
			return submit();
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left + 182, top + 90, 140, 22)) {
			closeWith(false);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 335) {
			return submit();
		}
		if (keyCode == 256 && client != null) {
			closeWith(false);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean submit() {
		if (submitting) {
			return true;
		}

		submitting = true;
		feedback = "";
		VipAccessManager.tryUnlockAsync(passwordField == null ? "" : passwordField.getText().trim())
			.whenComplete((result, throwable) -> {
				if (client == null) {
					return;
				}
				client.execute(() -> {
					submitting = false;
					if (throwable != null) {
						Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
						feedback = cause.getMessage() == null || cause.getMessage().isBlank() ? "Failed to contact VIP server." : cause.getMessage();
						feedbackColor = 0xFFE34B4B;
						return;
					}

					VipUnlockResult unlockResult = result;
					if (unlockResult != null && unlockResult.ok()) {
						KuzayClientContext context = KuzayClientModClient.getContext();
						if (context != null) {
							context.configManager().save(context);
						}
						closeWith(true);
						return;
					}

					feedback = unlockResult == null || unlockResult.message() == null || unlockResult.message().isBlank()
						? "Incorrect password."
						: unlockResult.message();
					feedbackColor = 0xFFE34B4B;
				});
			});
		return true;
	}

	private void closeWith(boolean unlocked) {
		if (resultConsumer != null) {
			resultConsumer.accept(unlocked);
		}
		if (client != null) {
			client.setScreen(parent);
		}
	}
}
