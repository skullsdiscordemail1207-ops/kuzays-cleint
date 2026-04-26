package com.kuzay2023.client.gui;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.gui.render.UiRenderer;
import com.kuzay2023.client.hud.HudElement;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {
	private final KuzayClientContext context;
	private HudElement draggedElement;
	private double dragOffsetX;
	private double dragOffsetY;

	public HudEditorScreen(KuzayClientContext context) {
		super(Text.literal("HUD Editor"));
		this.context = context;
	}

	@Override
	protected void init() {
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
	}

	@Override
	protected void applyBlur() {
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
	}

	@Override
	public void removed() {
		if (client != null) {
			client.gameRenderer.clearPostProcessor();
		}
		super.removed();
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		int transparencyAlpha = Math.max(88, Math.min(250, (int) (context.configManager().getConfig().theme.uiTransparency * 255.0F)));
		drawContext.fill(0, 0, width, height, UiRenderer.withAlpha(context.configManager().getConfig().theme.backgroundColor, Math.min(255, transparencyAlpha + 20)));

		int panelColor = UiRenderer.withAlpha(context.configManager().getConfig().theme.panelColor, transparencyAlpha);
		int accentColor = context.configManager().getConfig().theme.accentColor;
		int textColor = context.configManager().getConfig().theme.textColor;

		UiRenderer.drawPanel(drawContext, 18, 18, 260, 60, panelColor, accentColor);
		UiRenderer.drawText(drawContext, textRenderer, "HUD Editor", 32, 32, textColor);
		UiRenderer.drawText(drawContext, textRenderer, "Drag elements, right-click to toggle, scroll to resize.", 32, 46, UiRenderer.withAlpha(textColor, 150));

		for (HudElement element : context.hudManager().getElements()) {
			int elementWidth = (int) (element.getWidth() * element.getScale() * context.configManager().getConfig().layout.globalScale);
			int elementHeight = (int) (element.getHeight() * element.getScale() * context.configManager().getConfig().layout.globalScale);
			int outline = element.isEnabled() ? accentColor : UiRenderer.withAlpha(textColor, 110);
			int fill = UiRenderer.withAlpha(panelColor, 210);

			drawContext.fill((int) element.getX() - 2, (int) element.getY() - 2, (int) element.getX() + elementWidth + 2, (int) element.getY() + elementHeight + 2, fill);
			drawContext.drawBorder((int) element.getX() - 2, (int) element.getY() - 2, elementWidth + 4, elementHeight + 4, outline);

			if (element.isEnabled()) {
				drawContext.getMatrices().push();
				drawContext.getMatrices().translate(element.getX(), element.getY(), 0.0F);
				drawContext.getMatrices().scale(element.getScale() * context.configManager().getConfig().layout.globalScale, element.getScale() * context.configManager().getConfig().layout.globalScale, 1.0F);
				element.render(drawContext, context.configManager().getConfig().layout.globalScale);
				drawContext.getMatrices().pop();
			}

			UiRenderer.drawText(drawContext, textRenderer, element.getName(), (int) element.getX(), (int) element.getY() - 12, textColor);
		}

		super.render(drawContext, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (HudElement element : context.hudManager().getElements()) {
			if (!element.contains(mouseX, mouseY, context.configManager().getConfig().layout.globalScale)) {
				continue;
			}

			if (button == 0) {
				draggedElement = element;
				dragOffsetX = mouseX - element.getX();
				dragOffsetY = mouseY - element.getY();
			} else if (button == 1) {
				element.setEnabled(!element.isEnabled());
				context.hudManager().save();
			}
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (button == 0 && draggedElement != null) {
			draggedElement.setX((float) mouseX - (float) dragOffsetX);
			draggedElement.setY((float) mouseY - (float) dragOffsetY);
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggedElement != null) {
			context.hudManager().save();
			draggedElement = null;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (HudElement element : context.hudManager().getElements()) {
			if (element.contains(mouseX, mouseY, context.configManager().getConfig().layout.globalScale)) {
				element.setScale(Math.max(0.65F, Math.min(1.85F, element.getScale() + (float) verticalAmount * 0.05F)));
				context.hudManager().save();
				return true;
			}
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
