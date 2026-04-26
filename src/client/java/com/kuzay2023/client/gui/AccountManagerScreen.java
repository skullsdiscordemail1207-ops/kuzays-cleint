package com.kuzay2023.client.gui;

import java.util.List;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.account.LocalAccountManager;
import com.kuzay2023.client.config.LocalAccountConfig;
import com.kuzay2023.client.gui.render.UiRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AccountManagerScreen extends Screen {
	private final Screen parent;
	private final KuzayClientContext context;

	private TextFieldWidget labelField;
	private TextFieldWidget usernameField;
	private TextFieldWidget uuidField;
	private TextFieldWidget tokenField;
	private String feedback = "";
	private int selectedIndex = -1;
	private int listScroll;

	public AccountManagerScreen(Screen parent) {
		super(Text.literal("Accounts"));
		this.parent = parent;
		this.context = KuzayClientModClient.getContext();
	}

	@Override
	protected void init() {
		clearChildren();
		labelField = addField(width / 2 - 120, height / 2 - 84, 240, "Label");
		usernameField = addField(width / 2 - 120, height / 2 - 58, 240, "Username");
		uuidField = addField(width / 2 - 120, height / 2 - 32, 240, "UUID");
		tokenField = addField(width / 2 - 120, height / 2 - 6, 240, "Access Token");
		if (selectedIndex < 0 && !accounts().isEmpty()) {
			selectedIndex = 0;
			loadSelected();
		}
	}

	private TextFieldWidget addField(int x, int y, int width, String label) {
		TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 18, Text.literal(label));
		field.setMaxLength(256);
		addDrawableChild(field);
		return field;
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		drawContext.fill(0, 0, width, height, 0xE0101418);
		int left = width / 2 - 250;
		int top = height / 2 - 130;
		UiRenderer.drawPanel(drawContext, left, top, 500, 300, 0xEE1B2421, 0xFF324039);
		UiRenderer.drawText(drawContext, textRenderer, "Accounts", left + 18, top + 18, 0xFFF4F6F8);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, "Save and switch local session entries from inside multiplayer.", left + 18, top + 36, 460, 0xB4F4F6F8);

		int listTop = top + 130;
		int listHeight = 118;
		UiRenderer.drawPanel(drawContext, left + 18, listTop, 464, listHeight, 0xFF1D2623, 0xFF313B34);
		List<LocalAccountConfig> accounts = accounts();
		int contentHeight = Math.max(listHeight - 10, accounts.size() * 32);
		int maxScroll = Math.max(0, contentHeight - (listHeight - 10));
		listScroll = MathHelper.clamp(listScroll, 0, maxScroll);
		drawContext.enableScissor(left + 18, listTop, left + 482, listTop + listHeight);
		int rowY = listTop + 8 - listScroll;
		for (int i = 0; i < accounts.size(); i++) {
			LocalAccountConfig account = accounts.get(i);
			boolean selected = i == selectedIndex;
			UiRenderer.drawPanel(drawContext, left + 26, rowY, 448, 24, selected ? 0xAA2EC27E : 0xFF202824, selected ? 0xFF2EC27E : 0xFF313B34);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, account.label == null || account.label.isBlank() ? account.username : account.label, left + 36, rowY + 8, 180, 0xFFF4F6F8);
			UiRenderer.drawTrimmedText(drawContext, textRenderer, account.username, left + 220, rowY + 8, 160, 0xB4F4F6F8);
			rowY += 32;
		}
		drawContext.disableScissor();

		drawButton(drawContext, left + 266, top + 88, 92, 20, "Current", 0xFF2A3530);
		drawButton(drawContext, left + 364, top + 88, 92, 20, "Save", 0xFF2EC27E);
		drawButton(drawContext, left + 266, top + 248, 92, 20, "Switch", 0xFF2B7FFF);
		drawButton(drawContext, left + 364, top + 248, 92, 20, "Remove", 0xFFAA2E57);
		drawButton(drawContext, left + 18, top + 248, 92, 20, "Close", 0xFF2A3530);

		if (!feedback.isBlank()) {
			UiRenderer.drawTrimmedText(drawContext, textRenderer, feedback, left + 18, top + 276, 460, 0xFFE7F5EA);
		}
		super.render(drawContext, mouseX, mouseY, delta);
	}

	private void drawButton(DrawContext drawContext, int x, int y, int width, int height, String label, int color) {
		UiRenderer.drawPill(drawContext, x, y, width, height, color);
		UiRenderer.drawTrimmedText(drawContext, textRenderer, label, x + 18, y + 6, width - 36, 0xFFF4F6F8);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int left = width / 2 - 250;
		int top = height / 2 - 130;
		int listTop = top + 130;
		int rowY = listTop + 8 - listScroll;
		List<LocalAccountConfig> accounts = accounts();
		for (int i = 0; i < accounts.size(); i++) {
			if (UiRenderer.isHovered(mouseX, mouseY, left + 26, rowY, 448, 24)) {
				selectedIndex = i;
				loadSelected();
				return true;
			}
			rowY += 32;
		}

		if (UiRenderer.isHovered(mouseX, mouseY, left + 266, top + 88, 92, 20)) {
			fillFromCurrent();
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left + 364, top + 88, 92, 20)) {
			saveEntry();
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left + 266, top + 248, 92, 20)) {
			switchSelected();
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left + 364, top + 248, 92, 20)) {
			removeSelected();
			return true;
		}
		if (UiRenderer.isHovered(mouseX, mouseY, left + 18, top + 248, 92, 20) && client != null) {
			client.setScreen(parent);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int left = width / 2 - 250;
		int top = height / 2 - 130;
		if (UiRenderer.isHovered(mouseX, mouseY, left + 18, top + 130, 464, 118)) {
			listScroll = Math.max(0, listScroll - (int) (verticalAmount * 24.0));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private List<LocalAccountConfig> accounts() {
		return context == null ? List.of() : context.configManager().getConfig().accounts;
	}

	private void fillFromCurrent() {
		LocalAccountConfig current = LocalAccountManager.fromCurrentSession(MinecraftClient.getInstance());
		labelField.setText(current.label);
		usernameField.setText(current.username);
		uuidField.setText(current.uuid);
		tokenField.setText(current.accessToken);
		feedback = "Loaded current session into the form.";
	}

	private void saveEntry() {
		if (context == null) {
			return;
		}
		LocalAccountConfig entry = selectedIndex >= 0 && selectedIndex < accounts().size() ? accounts().get(selectedIndex) : new LocalAccountConfig();
		entry.label = labelField.getText().trim();
		entry.username = usernameField.getText().trim();
		entry.uuid = uuidField.getText().trim();
		entry.accessToken = tokenField.getText().trim();
		if (entry.username.isBlank()) {
			feedback = "Username is required.";
			return;
		}
		if (selectedIndex >= 0 && selectedIndex < accounts().size()) {
			accounts().set(selectedIndex, entry);
		} else {
			accounts().add(entry);
			selectedIndex = accounts().size() - 1;
		}
		LocalAccountManager.save(context);
		feedback = "Saved account entry.";
	}

	private void switchSelected() {
		if (selectedIndex < 0 || selectedIndex >= accounts().size()) {
			feedback = "Pick an account first.";
			return;
		}
		boolean applied = LocalAccountManager.applyAccount(MinecraftClient.getInstance(), accounts().get(selectedIndex));
		feedback = applied ? "Switched local client session." : "Could not switch account.";
	}

	private void removeSelected() {
		if (selectedIndex < 0 || selectedIndex >= accounts().size() || context == null) {
			return;
		}
		accounts().remove(selectedIndex);
		selectedIndex = accounts().isEmpty() ? -1 : Math.min(selectedIndex, accounts().size() - 1);
		if (selectedIndex >= 0) {
			loadSelected();
		} else {
			labelField.setText("");
			usernameField.setText("");
			uuidField.setText("");
			tokenField.setText("");
		}
		LocalAccountManager.save(context);
		feedback = "Removed account entry.";
	}

	private void loadSelected() {
		if (selectedIndex < 0 || selectedIndex >= accounts().size()) {
			return;
		}
		LocalAccountConfig entry = accounts().get(selectedIndex);
		labelField.setText(entry.label == null ? "" : entry.label);
		usernameField.setText(entry.username == null ? "" : entry.username);
		uuidField.setText(entry.uuid == null ? "" : entry.uuid);
		tokenField.setText(entry.accessToken == null ? "" : entry.accessToken);
	}
}
