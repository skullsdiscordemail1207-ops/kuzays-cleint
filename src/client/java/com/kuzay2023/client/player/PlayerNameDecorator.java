package com.kuzay2023.client.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kuzay2023.client.KuzayClientContext;
import com.kuzay2023.client.KuzayClientModClient;
import com.kuzay2023.client.freelook.FreeLookController;
import com.kuzay2023.client.module.Module;
import com.kuzay2023.client.module.impl.MediaRankModule;
import com.kuzay2023.client.module.impl.NametagsModule;
import com.kuzay2023.client.module.impl.VisualUsernameModule;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PlayerNameDecorator {
	private static final Pattern USERNAME_PATTERN = Pattern.compile("([.]?[A-Za-z0-9_]{3,16})");
	private static final Identifier MEDIA_BADGE_FONT = Identifier.of("kuzay2023sclient", "media_badge");
	private static final Text MEDIA_BADGE_TEXT = Text.literal("\uE000").setStyle(Style.EMPTY.withFont(MEDIA_BADGE_FONT));

	private PlayerNameDecorator() {
	}

	public static Text decorateEntityDisplayName(Entity entity, Text original) {
		if (!(entity instanceof PlayerEntity player) || original == null) {
			return original;
		}

		Text decorated = original;

		if (isVisualUsernameEnabled() && isLocalPlayer(player)) {
			String alias = getConfiguredAlias();
			if (alias != null && !alias.isBlank()) {
				decorated = rebuildText(decorated, player.getGameProfile().getName(), alias.trim());
			}
		}

		if (shouldApplyMediaBadge(player)) {
			decorated = prependMediaBadge(decorated);
		}

		if (shouldApplyNametags(player)) {
			decorated = appendNametagData(player, decorated);
		}

		return decorated;
	}

	public static Text decoratePlayerListName(GameProfile profile, Text original) {
		if (profile == null || !isVisualUsernameEnabled() || !isLocalProfile(profile)) {
			return original;
		}

		String alias = getConfiguredAlias();
		if (alias == null || alias.isBlank()) {
			return original;
		}

		Text baseText = original != null ? original : Text.literal(profile.getName());
		return rebuildText(baseText, profile.getName(), alias.trim());
	}

	public static boolean shouldRenderMediaBadge(int playerEntityId) {
		return playerEntityId >= 0
			&& FreeLookController.isActive()
			&& isMediaRankPreviewEnabled()
			&& FreeLookController.getTargetedEntityId() == playerEntityId;
	}

	public static Text getMediaBadgeText() {
		return MEDIA_BADGE_TEXT;
	}

	private static boolean isVisualUsernameEnabled() {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) return false;
		Module module = context.moduleManager().getModule("visual_username");
		return module instanceof VisualUsernameModule visualUsernameModule
			&& visualUsernameModule.isEnabled()
			&& !visualUsernameModule.getVisualUsername().isBlank();
	}

	private static boolean isMediaRankPreviewEnabled() {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) return false;
		Module module = context.moduleManager().getModule("media_rank_preview");
		return module instanceof MediaRankModule && module.isEnabled();
	}

	private static String getConfiguredAlias() {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) return "";
		Module module = context.moduleManager().getModule("visual_username");
		return module instanceof VisualUsernameModule visualUsernameModule
			? visualUsernameModule.getVisualUsername()
			: "";
	}

	private static Module getModule(String moduleId) {
		KuzayClientContext context = KuzayClientModClient.getContext();
		return context == null ? null : context.moduleManager().getModule(moduleId);
	}

	private static boolean isLocalPlayer(PlayerEntity player) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}
		if (client.player != null && player.getUuid().equals(client.player.getUuid())) {
			return true;
		}
		return player.getGameProfile().getName().equals(client.getSession().getUsername());
	}

	private static boolean isLocalProfile(GameProfile profile) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}
		if (client.player != null && profile.getId() != null && profile.getId().equals(client.player.getUuid())) {
			return true;
		}
		return profile.getName().equals(client.getSession().getUsername());
	}

	private static String replaceUsernameToken(String rawText, String baseName, String alias) {
		Matcher matcher = USERNAME_PATTERN.matcher(rawText);
		StringBuffer buffer = new StringBuffer();
		boolean replaced = false;
		while (matcher.find()) {
			String candidate = matcher.group(1);
			if (!replaced && (candidate.equals(baseName) || candidate.equals("." + baseName))) {
				matcher.appendReplacement(buffer, Matcher.quoteReplacement(alias));
				replaced = true;
			}
		}
		matcher.appendTail(buffer);
		return replaced ? buffer.toString() : rawText;
	}

	private static Text rebuildText(Text original, String baseName, String alias) {
		MutableText updated = Text.empty();
		boolean[] replaced = {false};
		boolean[] appended = {false};

		original.visit((style, segment) -> {
			String updatedSegment = replaceUsernameToken(segment, baseName, alias);
			if (!updatedSegment.equals(segment)) {
				replaced[0] = true;
			}

			if (!updatedSegment.isEmpty()) {
				MutableText piece = Text.literal(updatedSegment);
				piece.setStyle(style);
				updated.append(piece);
				appended[0] = true;
			}

			return java.util.Optional.empty();
		}, Style.EMPTY.withParent(original.getStyle()));

		if (!replaced[0]) {
			return original;
		}

		if (!appended[0]) {
			MutableText aliasText = Text.literal(alias);
			aliasText.setStyle(original.getStyle());
			return aliasText;
		}

		return updated;
	}

	private static boolean shouldApplyMediaBadge(PlayerEntity player) {
		if (!FreeLookController.isActive() || !isMediaRankPreviewEnabled()) {
			return false;
		}

		if (isLocalPlayer(player)) {
			return true;
		}

		UUID targetedUuid = FreeLookController.getTargetedPlayerUuid();
		return player.getId() == FreeLookController.getTargetedEntityId()
			|| (targetedUuid != null && targetedUuid.equals(player.getUuid()));
	}

	private static boolean shouldApplyNametags(Entity entity) {
		KuzayClientContext context = KuzayClientModClient.getContext();
		MinecraftClient client = MinecraftClient.getInstance();
		if (context == null || client == null || client.player == null) {
			return false;
		}
		Module module = context.moduleManager().getModule("nametags");
		return module instanceof NametagsModule nametagsModule
			&& module.isEnabled()
			&& nametagsModule.shouldShow(entity, client.player);
	}

	private static Text appendNametagData(Entity entity, Text baseText) {
		KuzayClientContext context = KuzayClientModClient.getContext();
		if (context == null) {
			return baseText;
		}
		Module module = context.moduleManager().getModule("nametags");
		if (!(module instanceof NametagsModule nametagsModule)) {
			return baseText;
		}

		MutableText updated = baseText.copy();
		if (entity instanceof LivingEntity living && nametagsModule.shouldShowHealth()) {
			updated.append(Text.literal(" " + Math.round(living.getHealth())).setStyle(Style.EMPTY.withColor(0xFF4ADE80)));
		}

		if (entity instanceof PlayerEntity player) {
			if (nametagsModule.shouldShowHeldItem()) {
				String heldItem = summarizeStack(player.getMainHandStack(), nametagsModule.shouldShowEnchants());
				if (!heldItem.isBlank()) {
					updated.append(Text.literal(" [" + heldItem + "]").setStyle(Style.EMPTY.withColor(0xFFF4F6F8)));
				}
			}
			if (nametagsModule.shouldShowArmor()) {
				String armorSummary = summarizeArmor(player, nametagsModule.shouldShowEnchants());
				if (!armorSummary.isBlank()) {
					updated.append(Text.literal(" {" + armorSummary + "}").setStyle(Style.EMPTY.withColor(0xFF9FDBFF)));
				}
			}
		}

		return updated;
	}

	private static String summarizeArmor(PlayerEntity player, boolean showEnchants) {
		List<String> parts = new ArrayList<>();
		for (ItemStack stack : player.getArmorItems()) {
			String summary = summarizeStack(stack, showEnchants);
			if (!summary.isBlank()) {
				parts.add(summary);
			}
		}
		return String.join(" | ", parts);
	}

	private static String summarizeStack(ItemStack stack, boolean showEnchants) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder(stack.getName().getString());
		if (showEnchants) {
			List<String> enchants = summarizeEnchants(stack);
			if (!enchants.isEmpty()) {
				builder.append(' ');
				builder.append(String.join(",", enchants));
			}
		}
		return builder.toString();
	}

	private static List<String> summarizeEnchants(ItemStack stack) {
		List<String> enchants = new ArrayList<>();
		try {
			Object enchantmentComponent = EnchantmentHelper.getEnchantments(stack);
			for (java.lang.reflect.Method method : enchantmentComponent.getClass().getMethods()) {
				if (!method.getName().equals("toString")) {
					continue;
				}
				String raw = String.valueOf(method.invoke(enchantmentComponent)).toLowerCase(Locale.ROOT);
				if (raw.isBlank()) {
					continue;
				}
				if (raw.contains("protection")) enchants.add("P");
				if (raw.contains("blast_protection")) enchants.add("BP");
				if (raw.contains("unbreaking")) enchants.add("U");
				if (raw.contains("mending")) enchants.add("M");
				if (raw.contains("sharpness")) enchants.add("S");
				if (raw.contains("efficiency")) enchants.add("E");
				break;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return enchants;
	}

	private static Text prependMediaBadge(Text original) {
		return Text.empty()
			.append(MEDIA_BADGE_TEXT.copy())
			.append(original.copy());
	}
}
