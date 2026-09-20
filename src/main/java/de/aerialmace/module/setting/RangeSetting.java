package de.aerialmace.module.setting;

import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Range setting with an independent minimum and maximum (e.g. random delay bounds).
 * The GUI renders it as a bar with two draggable handles; both writes go straight into
 * the backing configuration, so the combat logic uses the new bounds immediately.
 *
 * <p>The two values are always clamped into {@code [min, max]} and the minimum can never
 * cross the maximum, so a hand-edited or outdated config file can neither push a handle off
 * the bar nor produce a reversed range. The writer is never called from the constructor —
 * the owner decides when the initial value is written, which keeps the backing config free
 * of side effects during GUI construction.
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
		this.step = Math.max(0.0001, step);
		// Values coming from a config file may lie outside the GUI range, so clamp the
		// defaults as well: the handles can then never leave the track.
		this.defaultMin = snapInBounds(defaultMin);
		this.defaultMax = Math.max(this.defaultMin, snapInBounds(defaultMax));
		this.unit = unit;
		this.writer = writer;
		this.minValue = this.defaultMin;
		this.maxValue = this.defaultMax;
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

	private double snap(double value) {
		return Math.round(value / step) * step;
	}

	/** Snaps a value to the step and clamps it into the GUI bounds. */
	private double snapInBounds(double value) {
		return Math.max(min, Math.min(max, snap(value)));
	}

	/**
	 * Applies both bounds at once. Used when the backing configuration changed outside the
	 * GUI (config load, profile switch, cloud config) so the handles show the live values.
	 */
	public void setRange(double newMin, double newMax) {
		double lo = snapInBounds(newMin);
		double hi = snapInBounds(newMax);
		if (hi < lo) {
			double swap = lo;
			lo = hi;
			hi = swap;
		}
		if (minValue != lo || maxValue != hi) {
			minValue = lo;
			maxValue = hi;
			writer.accept(getIntMinValue(), getIntMaxValue());
			fireChanged();
		}
	}

	public void setMinValue(double newMin) {
		double snapped = snapInBounds(newMin);
		// The minimum must never exceed the maximum.
		snapped = Math.min(snapped, maxValue);
		if (minValue != snapped) {
			minValue = snapped;
			writer.accept(getIntMinValue(), getIntMaxValue());
			fireChanged();
		}
	}

	public void setMaxValue(double newMax) {
		double snapped = snapInBounds(newMax);
		// The maximum must never fall below the minimum.
		snapped = Math.max(snapped, minValue);
		if (maxValue != snapped) {
			maxValue = snapped;
			writer.accept(getIntMinValue(), getIntMaxValue());
			fireChanged();
		}
	}

	@Override
	public void reset() {
		setRange(defaultMin, defaultMax);
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
			double storedMin = object.has("min") ? object.get("min").getAsDouble() : minValue;
			double storedMax = object.has("max") ? object.get("max").getAsDouble() : maxValue;
			// Applied in one step so a reversed pair from a hand-edited file is fixed up
			// instead of being clamped one value at a time.
			setRange(storedMin, storedMax);
		}
	}
}
