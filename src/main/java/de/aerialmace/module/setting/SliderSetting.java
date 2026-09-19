package de.aerialmace.module.setting;

import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Single-value slider setting. {@code writer} receives the snapped value and writes it
 * straight into the backing configuration.
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
		this.step = step;
		this.defaultValue = defaultValue;
		this.unit = unit;
		this.writer = writer;
		this.value = defaultValue;
		writer.accept(value, value);
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
		double snapped = Math.round(newValue / step) * step;
		snapped = Math.max(min, Math.min(max, snapped));
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
