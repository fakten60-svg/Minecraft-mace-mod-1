package de.aerialmace.gui;

import de.aerialmace.module.setting.ColorSetting;

/**
 * Central theme manager. GUI code never hardcodes colors; everything reads from here and
 * updates automatically when a {@link ColorSetting} changes.
 */
public final class ThemeManager {

	public enum Theme {
		DARK, MIDNIGHT, MONO
	}

	private static final ThemeManager INSTANCE = new ThemeManager();

	/** Global access point used by the GUI and the config layer. */
	public static ThemeManager get() {
		return INSTANCE;
	}

	private final ColorSetting accent = new ColorSetting("Accent Color", 0xFF3B82F6);
	private final ColorSetting background = new ColorSetting("Background Color", 0xC0101014);
	private final ColorSetting panel = new ColorSetting("Panel Color", 0xE016161C);
	private final ColorSetting moduleActive = new ColorSetting("Module Active Color", 0xFF22303F);
	private final ColorSetting text = new ColorSetting("Text Color", 0xFFF2F2F2);
	private final ColorSetting secondaryText = new ColorSetting("Secondary Text Color", 0xFF9A9AA5);

	public ColorSetting accent() {
		return accent;
	}

	public ColorSetting background() {
		return background;
	}

	public ColorSetting panel() {
		return panel;
	}

	public ColorSetting moduleActive() {
		return moduleActive;
	}

	public ColorSetting text() {
		return text;
	}

	public ColorSetting secondaryText() {
		return secondaryText;
	}

	/** Derived surface color (slightly lighter than the panel) for knobs/inputs. */
	public int surface() {
		return lerp(panel.getColor(), 0xFF2A2A33, 0.35f);
	}

	/** Derived hover overlay (subtle white wash). */
	public int hoverOverlay() {
		return withAlpha(0xFFFFFFFF, 0x14);
	}

	/** ARGB int helpers used all over the GUI. */
	public static int withAlpha(int argb, int alpha) {
		return (clamp(alpha) << 24) | (argb & 0x00FFFFFF);
	}

	public static int lerp(int fromArgb, int toArgb, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int a = lerpChannel((fromArgb >>> 24) & 0xFF, (toArgb >>> 24) & 0xFF, t);
		int r = lerpChannel((fromArgb >> 16) & 0xFF, (toArgb >> 16) & 0xFF, t);
		int g = lerpChannel((fromArgb >> 8) & 0xFF, (toArgb >> 8) & 0xFF, t);
		int b = lerpChannel(fromArgb & 0xFF, toArgb & 0xFF, t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float t) {
		return Math.round(from + (to - from) * t);
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}
}
