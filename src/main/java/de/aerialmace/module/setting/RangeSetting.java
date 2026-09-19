package de.aerialmace.module.setting;

import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Range setting with an independent minimum and maximum (e.g. random delay bounds).
 * The GUI renders it as a bar with two draggable handles; both writes go straight into
 * the backing configuration, so the combat logic uses the new bounds immediately.
 */
public class RangeSetting extends Setting {

	private final double min;
	private final double max;
	private final double step;
	private final double defaultMin;
	private final double defaultMax;
	private final String unit;
	private final BiConsumer<Integer, Integer> writer; // (minValue, maxValue)
	private double minValue;
	private double maxValue;

	public RangeSetting(String name, String unit, double min, double max, double defaultMin, double defaultMax,
			double step, BiConsumer<Integer, Integer> writer) {
		super(name);
		this.min = min;
		this.max = max;
		this.step = step;
		this.defaultMin = defaultMin;
		this.defaultMax = defaultMax;
		this.unit = unit;
		this.writer = writer;
		this.minValue = defaultMin;
		this.maxValue = defaultMax;
		writer.accept((int) Math.round(minValue), (int) Math.round(maxValue));
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	public double getMinValue() {
		return minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public int getIntMinValue() {
		return (int) Math.round(minValue);
	}

	public int getIntMaxValue() {
		return (int) Math.round(maxValue);
	}

	public String getUnit() {
		return unit;
	}

	private double snap(double v) {
		return Math.round(v / step) * step;
	}

	public void setMinValue(double newMin) {
		newMin = Math.max(min, Math.min(max, snap(newMin)));
		// The minimum must never exceed the maximum.
		newMin = Math.min(newMin, maxValue);
		if (minValue != newMin) {
			minValue = newMin;
			writer.accept(getIntMinValue(), getIntMaxValue());
			fireChanged();
		}
	}

	public void setMaxValue(double newMax) {
		newMax = Math.max(min, Math.min(max, snap(newMax)));
		// The maximum must never fall below the minimum.
		newMax = Math.max(newMax, minValue);
		if (maxValue != newMax) {
			maxValue = newMax;
			writer.accept(getIntMinValue(), getIntMaxValue());
			fireChanged();
		}
	}

	@Override
	public void reset() {
		minValue = defaultMin;
		maxValue = defaultMax;
		writer.accept(getIntMinValue(), getIntMaxValue());
		fireChanged();
	}

	@Override
	public JsonElement toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("min", minValue);
		object.addProperty("max", maxValue);
		return object;
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			if (object.has("max")) {
				setMaxValue(object.get("max").getAsDouble());
			}
			if (object.has("min")) {
				setMinValue(object.get("min").getAsDouble());
			}
		}
	}
}
