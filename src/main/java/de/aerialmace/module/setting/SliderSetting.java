package de.aerialmace.module.setting;

import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Single-value slider setting. {@code writer} receives the snapped value and writes it
 * straight into the backing configuration.
 *
 * <p>The writer is only called on real changes: constructing a setting has no side effect on
 * the backing configuration, so the owner decides when the initial value is written.
 */
public class SliderSetting extends Setting {

	private final double min;
	private final double max;
	private final double step;
	private final double defaultValue;
	private final String unit;
	private final BiConsumer<Double, Double> writer; // (value, unused) for uniform signature
	private double value;

	public SliderSetting(String name, String unit, double min, double max, double defaultValue, double step,
			BiConsumer<Double, Double> writer) {
		super(name);
		this.min = min;
		this.max = max;
		this.step = Math.max(0.0001, step);
		this.defaultValue = defaultValue;
		this.unit = unit;
		this.writer = writer;
		this.value = snapInBounds(defaultValue);
	}

	/** Snaps a value to the step and clamps it into the slider bounds. */
	private double snapInBounds(double value) {
		return Math.max(min, Math.min(max, Math.round(value / step) * step));
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

	public double getValue() {
		return value;
	}

	public int getIntValue() {
		return (int) Math.round(value);
	}

	public String getUnit() {
		return unit;
	}

	public void setValue(double newValue) {
		double snapped = snapInBounds(newValue);
		if (this.value != snapped) {
			this.value = snapped;
			writer.accept(snapped, snapped);
			fireChanged();
		}
	}

	@Override
	public void reset() {
		setValue(defaultValue);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			setValue(element.getAsDouble());
		}
	}
}
