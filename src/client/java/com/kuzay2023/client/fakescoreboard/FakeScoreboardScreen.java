package com.kuzay2023.client.fakescoreboard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FakeScoreboardScreen extends Screen {
	private final Screen parent;
	private TextFieldWidget moneyField;

	public FakeScoreboardScreen(Screen parent) {
		super(Text.literal("Fake Scoreboard"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		int centerX = width / 2;
		int centerY = height / 2;

		moneyField = new TextFieldWidget(textRenderer, centerX - 100, centerY - 30, 200, 20, Text.literal("Money"));
		moneyField.setText(FakeScoreboardConfig.formatMoney(config.getCurrentMoney()));
		moneyField.setMaxLength(32);
		addDrawableChild(moneyField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
			.dimensions(centerX - 100, centerY + 5, 98, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal(config.isEnabled() ? "Disable Money Override" : "Enable Money Override"), button -> {
			config.setEnabled(!config.isEnabled());
			config.save();
			MinecraftClient.getInstance().setScreen(new FakeScoreboardScreen(parent));
		}).dimensions(centerX + 2, centerY + 5, 98, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
			.dimensions(centerX - 100, centerY + 30, 200, 20)
			.build());

		setInitialFocus(moneyField);
	}

	private void saveAndClose() {
		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		try {
			config.setCurrentMoney(MoneyParser.parseAmount(moneyField.getText()));
			config.save();
			close();
		} catch (NumberFormatException exception) {
			moneyField.setEditableColor(0xFF5555);
		}
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 60, 0xFFFFFF);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal("Set the fake money value used on the real scoreboard"), width / 2, height / 2 - 45, 0xAAAAAA);
		context.drawTextWithShadow(textRenderer, Text.literal("Balance"), width / 2 - 100, height / 2 - 20, 0xFFFFFF);
		moneyField.render(context, mouseX, mouseY, delta);
	}
}
