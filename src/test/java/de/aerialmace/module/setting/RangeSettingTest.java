package de.aerialmace.module.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Targeted tests for the two-handle range setting that backs the random-delay bars:
 * min can never cross max, values are snapped and clamped, serialization round-trips
 * and the backing-config writer is only called on real changes.
 */
class RangeSettingTest {

	private final AtomicInteger writerMin = new AtomicInteger(-1);
	private final AtomicInteger writerMax = new AtomicInteger(-1);
	private int writerCalls;

	private RangeSetting newRange() {
		return new RangeSetting("Initial Delay", "ms", 0, 3000, 100, 120, 1, (min, max) -> {
			writerMin.set(min);
			writerMax.set(max);
			writerCalls++;
		});
	}

	@Test
	void constructorDoesNotCallTheWriter() {
		newRange();
		assertEquals(0, writerCalls, "constructing a setting must not touch the backing config");
	}

	@Test
	void minNeverCrossesMax() {
		RangeSetting range = newRange();
		range.setMinValue(200); // above the current max of 120
		assertEquals(120, range.getIntMinValue());
		assertEquals(120, range.getIntMaxValue());
	}

	@Test
	void maxNeverFallsBelowMin() {
		RangeSetting range = newRange();
		range.setMaxValue(50); // below the current min of 100
		assertEquals(100, range.getIntMaxValue());
		assertEquals(100, range.getIntMinValue());
	}

	@Test
	void valuesAreClampedIntoGuiBounds() {
		RangeSetting range = newRange();
		range.setMinValue(-100);
		range.setMaxValue(9999);
		assertEquals(0, range.getIntMinValue());
		assertEquals(3000, range.getIntMaxValue());
	}

	@Test
	void valuesSnapToStep() {
		RangeSetting range = newRange();
		range.setMaxValue(117.6);
		assertEquals(118, range.getIntMaxValue());
	}

	@Test
	void writerOnlyCalledOnRealChanges() {
		RangeSetting range = newRange();
		range.setMinValue(100); // unchanged
		assertEquals(0, writerCalls);
		range.setMinValue(108);
		assertEquals(1, writerCalls);
		assertEquals(108, writerMin.get());
		assertEquals(120, writerMax.get());
	}

	@Test
	void setRangeSwapsReversedPairs() {
		RangeSetting range = newRange();
		range.setRange(150, 110);
		assertEquals(110, range.getIntMinValue());
		assertEquals(150, range.getIntMaxValue());
	}

	@Test
	void resetRestoresDefaults() {
		RangeSetting range = newRange();
		range.setMinValue(10);
		range.setMaxValue(2000);
		range.reset();
		assertEquals(100, range.getIntMinValue());
		assertEquals(120, range.getIntMaxValue());
	}

	@Test
	void jsonRoundTrip() {
		RangeSetting range = newRange();
		range.setMinValue(105);
		range.setMaxValue(117);

		RangeSetting other = newRange();
		other.fromJson(range.toJson());
		assertEquals(105, other.getIntMinValue());
		assertEquals(117, other.getIntMaxValue());
	}

	@Test
	void fromJsonToleratesGarbage() {
		RangeSetting range = newRange();
		range.fromJson(new com.google.gson.JsonPrimitive("not-an-object"));
		assertEquals(100, range.getIntMinValue(), "garbage input must keep the current value");
		range.fromJson(null);
		assertEquals(100, range.getIntMinValue());
	}

	@Test
	void fromJsonFixesReversedStoredPair() {
		RangeSetting range = newRange();
		com.google.gson.JsonObject stored = new com.google.gson.JsonObject();
		stored.addProperty("min", 180);
		stored.addProperty("max", 120);
		range.fromJson(stored);
		assertEquals(120, range.getIntMinValue());
		assertEquals(180, range.getIntMaxValue());
	}

	@Test
	void defaultsFromConfigOutsideGuiRangeAreClamped() {
		// Simulates a hand-edited config: defaults themselves must land on the track.
		RangeSetting range = new RangeSetting("X", "ms", 0, 3000, -50, 9999, 1, (a, b) -> {
		});
		assertTrue(range.getIntMinValue() >= 0);
		assertEquals(3000, range.getIntMaxValue());
	}
}
