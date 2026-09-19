package de.aerialmace.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Small drawing helpers shared by all GUI components (flat, modern look with 1px
 * borders and alpha blending — no heavy effects).
 */
public final class GuiRenderUtil {

	private GuiRenderUtil() {
	}

	/** Rectangle with a 1px border. */
	public static void borderedRect(DrawContext context, int x, int y, int width, int height, int fill, int border) {
		context.fill(x, y, x + width, y + height, border);
		context.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
	}

	/** Horizontal gradient (fillGradient is vertical only), one fill per column. */
	public static void horizontalGradient(DrawContext context, int x, int y, int width, int height,
			int fromColor, int toColor) {
		if (width <= 0) {
			return;
		}
		for (int i = 0; i < width; i++) {
			float t = width == 1 ? 0.0f : (float) i / (width - 1);
			context.fill(x + i, y, x + i + 1, y + height, ThemeManager.lerp(fromColor, toColor, t));
		}
	}

	/** Rainbow strip for the color picker hue slider. */
	public static void hueStrip(DrawContext context, int x, int y, int width, int height) {
		int steps = Math.max(height, 24);
		for (int i = 0; i < steps; i++) {
			float hue = (float) i / (steps - 1);
			int color = hsvToRgb(hue * 360.0f, 1.0f, 1.0f) | 0xFF000000;
			int sy = y + (int) ((float) i / steps * height);
			int sy2 = y + (int) ((float) (i + 1) / steps * height);
			context.fill(x, sy, x + width, Math.max(sy + 1, sy2), color);
		}
	}

	/** Converts HSV (h 0-360, s/v 0-1) to an RGB int. */
	public static int hsvToRgb(float hue, float saturation, float value) {
		hue = ((hue % 360.0f) + 360.0f) % 360.0f;
		int h = (int) (hue / 60.0f) % 6;
		float f = hue / 60.0f - h;
		float p = value * (1 - saturation);
		float q = value * (1 - f * saturation);
		float t = value * (1 - (1 - f) * saturation);
		return switch (h) {
			case 0 -> rgb(value, t, p);
			case 1 -> rgb(q, value, p);
			case 2 -> rgb(p, value, t);
			case 3 -> rgb(p, q, value);
			case 4 -> rgb(t, p, value);
			default -> rgb(value, p, q);
		};
	}

	private static int rgb(float r, float g, float b) {
		int ri = Math.max(0, Math.min(255, Math.round(r * 255)));
		int gi = Math.max(0, Math.min(255, Math.round(g * 255)));
		int bi = Math.max(0, Math.min(255, Math.round(b * 255)));
		return (ri << 16) | (gi << 8) | bi;
	}
}
