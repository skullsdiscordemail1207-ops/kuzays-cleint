package meteordevelopment.meteorclient.renderer;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.render.FontUtils;

public class Fonts {
	public static final String[] BUILTIN_FONTS = {
		"JetBrains Mono",
		"Comfortaa",
		"Tw Cen MT",
		"Pixelation"
	};

	public static String DEFAULT_FONT_FAMILY;
	public static FontFace DEFAULT_FONT;
	public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
	public static CustomTextRenderer RENDERER;

	private static FontFace queuedFont;
	private static FontFace failedFont;

	private Fonts() {
	}

	public static void refresh() {
		FONT_FAMILIES.clear();

		for (String builtinFont : BUILTIN_FONTS) {
			FontUtils.loadBuiltin(FONT_FAMILIES, builtinFont);
		}

		for (String searchPath : FontUtils.getSearchPaths()) {
			FontUtils.loadSystem(FONT_FAMILIES, new File(searchPath));
		}

		FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));
		MeteorClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());

		resolveDefaultFont();
		failedFont = null;

		Config config = Config.get();
		FontFace configuredFont = config != null ? config.font.get() : DEFAULT_FONT;
		load(configuredFont);
	}

	public static void load(FontFace fontFace) {
		FontFace targetFont = resolveTargetFont(fontFace);
		if (targetFont == null) {
			return;
		}

		if (RENDERER != null && RENDERER.fontFace.equals(targetFont)) {
			queuedFont = null;
			failedFont = null;
			invalidateWidgetScreen();
			return;
		}

		queuedFont = targetFont;
		ensureLoaded();
		invalidateWidgetScreen();
	}

	public static void ensureLoaded() {
		tryLoadQueuedFont();
	}

	public static FontFamily getFamily(String name) {
		for (FontFamily family : FONT_FAMILIES) {
			if (family.getName().equalsIgnoreCase(name)) {
				return family;
			}
		}

		return null;
	}

	private static void resolveDefaultFont() {
		FontInfo builtinInfo = FontUtils.getBuiltinFontInfo(BUILTIN_FONTS[1]);
		if (builtinInfo != null) {
			DEFAULT_FONT_FAMILY = builtinInfo.family();
		} else if (!FONT_FAMILIES.isEmpty()) {
			DEFAULT_FONT_FAMILY = FONT_FAMILIES.get(0).getName();
		} else {
			DEFAULT_FONT_FAMILY = null;
		}

		FontFamily defaultFamily = DEFAULT_FONT_FAMILY != null ? getFamily(DEFAULT_FONT_FAMILY) : null;
		if (defaultFamily == null && !FONT_FAMILIES.isEmpty()) {
			defaultFamily = FONT_FAMILIES.get(0);
		}

		DEFAULT_FONT = defaultFamily != null ? defaultFamily.get(FontInfo.Type.Regular) : null;
	}

	private static FontFace resolveTargetFont(FontFace fontFace) {
		if (fontFace != null) {
			return fontFace;
		}

		if (DEFAULT_FONT == null) {
			resolveDefaultFont();
		}

		return DEFAULT_FONT;
	}

	private static void tryLoadQueuedFont() {
		FontFace targetFont = resolveTargetFont(queuedFont);
		if (targetFont == null || (failedFont != null && failedFont.equals(targetFont)) || !canCreateRenderer()) {
			return;
		}

		if (RENDERER != null && RENDERER.fontFace.equals(targetFont)) {
			queuedFont = null;
			failedFont = null;
			return;
		}

		CustomTextRenderer previousRenderer = RENDERER;
		try {
			RENDERER = new CustomTextRenderer(targetFont);
			queuedFont = null;
			failedFont = null;

			if (previousRenderer != null) {
				previousRenderer.destroy();
			}

			MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());
		} catch (Throwable t) {
			RENDERER = previousRenderer;
			failedFont = targetFont;

			if (DEFAULT_FONT != null && !targetFont.equals(DEFAULT_FONT)) {
				queuedFont = DEFAULT_FONT;
			} else {
				queuedFont = null;
			}

			MeteorClient.LOG.warn("Failed to load custom font renderer for {}. Using vanilla text fallback for now.", targetFont, t);
		}
	}

	private static boolean canCreateRenderer() {
		if (MeteorClient.mc == null || MeteorClient.mc.getWindow() == null) {
			return false;
		}

		try {
			return GLFW.glfwGetCurrentContext() != 0L && GL.getCapabilities() != null;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void invalidateWidgetScreen() {
		if (MeteorClient.mc == null || !(MeteorClient.mc.currentScreen instanceof WidgetScreen screen)) {
			return;
		}

		Config config = Config.get();
		if (config != null && Boolean.TRUE.equals(config.customFont.get())) {
			screen.invalidate();
		}
	}
}
