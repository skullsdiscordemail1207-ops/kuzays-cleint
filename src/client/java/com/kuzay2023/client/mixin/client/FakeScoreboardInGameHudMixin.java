package com.kuzay2023.client.mixin.client;

import java.util.Comparator;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.kuzay2023.client.fakescoreboard.FakeScoreboardClientHooks;
import com.kuzay2023.client.fakescoreboard.FakeScoreboardConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Mixin(InGameHud.class)
public abstract class FakeScoreboardInGameHudMixin {
	private static final Comparator<ScoreboardEntry> FAKE_SCOREBOARD_COMPARATOR =
		Comparator.comparingInt(ScoreboardEntry::value)
			.reversed()
			.thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER.reversed());

	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
	private void kuzayClient$renderFakeSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo info) {
		FakeScoreboardConfig config = FakeScoreboardClientHooks.getConfig();
		if (config == null || !config.isEnabled() || client.player == null) {
			return;
		}

		info.cancel();

		Scoreboard scoreboard = objective.getScoreboard();
		NumberFormat fallbackFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);
		List<SidebarLine> lines = scoreboard.getScoreboardEntries(objective).stream()
			.filter(entry -> !entry.hidden())
			.sorted(FAKE_SCOREBOARD_COMPARATOR)
			.limit(15)
			.map(entry -> kuzayClient$toLine(scoreboard, entry, fallbackFormat, config))
			.toList();

		TextRenderer renderer = client.textRenderer;
		Text title = objective.getDisplayName();
		int width = renderer.getWidth(title);
		for (SidebarLine line : lines) {
			width = Math.max(width, renderer.getWidth(line.label()) + renderer.getWidth(line.value()) + 2);
		}

		int lineHeight = 9;
		int totalHeight = lines.size() * lineHeight;
		int startY = context.getScaledWindowHeight() / 2 + totalHeight / 3;
		int right = context.getScaledWindowWidth() - 3;
		int left = right - width - 4;
		int titleTop = startY - totalHeight - lineHeight - 1;

		context.fill(left - 2, titleTop - 1, right, startY, 0x50000000);
		context.fill(left - 2, titleTop - 1, right, titleTop + lineHeight - 1, 0x60000000);
		context.drawCenteredTextWithShadow(renderer, title, (left + right - 2) / 2, titleTop, 0xFFFFFF);

		int y = startY - totalHeight;
		for (SidebarLine line : lines) {
			context.fill(left - 2, y, right, y + lineHeight, 0x30000000);
			context.drawTextWithShadow(renderer, line.label(), left, y, 0xFFFFFF);
			context.drawTextWithShadow(renderer, line.value(), right - renderer.getWidth(line.value()) - 2, y, 0xFFFFFF);
			y += lineHeight;
		}
	}

	private SidebarLine kuzayClient$toLine(Scoreboard scoreboard, ScoreboardEntry entry, NumberFormat fallbackFormat, FakeScoreboardConfig config) {
		Team team = scoreboard.getScoreHolderTeam(entry.owner());
		Text label = entry.display() != null ? entry.display() : Team.decorateName(team, entry.name());
		if (kuzayClient$isMoneyEntry(entry, label)) {
			MutableText moneyLabel = Text.literal("$").formatted(Formatting.GREEN)
				.append(Text.literal(" Money ").formatted(Formatting.WHITE))
				.append(Text.literal(FakeScoreboardConfig.formatMoney(config.getCurrentMoney())).formatted(Formatting.GREEN));
			return new SidebarLine(moneyLabel, Text.empty());
		}

		return new SidebarLine(label, entry.formatted(fallbackFormat));
	}

	private boolean kuzayClient$isMoneyEntry(ScoreboardEntry entry, Text label) {
		String combined = (label.getString() + " " + entry.name().getString() + " " + entry.owner()).toLowerCase();
		return combined.contains("money");
	}

	private record SidebarLine(Text label, Text value) {
	}
}
