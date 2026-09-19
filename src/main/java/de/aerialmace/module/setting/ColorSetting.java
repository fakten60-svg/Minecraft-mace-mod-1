package de.aerialmace.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Color setting (RGB, alpha channel supported for transparency colors).
 */
public class ColorSetting extends Setting {

	private final int defaultColor;
	private int color;

	public ColorSetting(String name, int defaultColor) {
		super(name);
		this.defaultColor = defaultColor;
		this.color = defaultColor;
	}

	/** Returns the color as ARGB int. */
	public int getColor() {
		return color;
	}

	public int getRed() {
		return (color >> 16) & 0xFF;
	}

	public int getGreen() {
		return (color >> 8) & 0xFF;
	}

	public int getBlue() {
		return color & 0xFF;
	}

	public int getAlpha() {
		return (color >>> 24) & 0xFF;
	}

	public void setColor(int red, int green, int blue, int alpha) {
		int newColor = (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
		if (color != newColor) {
			color = newColor;
			fireChanged();
		}
	}

	public void setColor(int argb) {
		setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	@Override
	public void reset() {
		color = -1; // force write
		setColor(defaultColor);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(color);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			color = element.getAsInt(); // direct set; values were clamped on save
		}
	}
}
