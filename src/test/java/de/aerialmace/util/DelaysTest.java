package de.aerialmace.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Targeted tests for the random delay helper used by the combat state machine.
 */
class DelaysTest {

	@Test
	void staysWithinInclusiveRange() {
		for (int i = 0; i < 10_000; i++) {
			int delay = Delays.randomDelay(100, 120);
			assertTrue(delay >= 100 && delay <= 120, "delay outside range: " + delay);
		}
	}

	@Test
	void swappedArgumentsBehaveTheSame() {
		for (int i = 0; i < 1_000; i++) {
			int delay = Delays.randomDelay(90, 67);
			assertTrue(delay >= 67 && delay <= 90, "delay outside range: " + delay);
		}
	}

	@Test
	void negativeArgumentsAreClampedToZero() {
		for (int i = 0; i < 100; i++) {
			int delay = Delays.randomDelay(-50, -10);
			assertEquals(0, delay);
		}
	}

	@Test
	void equalBoundsReturnExactlyThatValue() {
		assertEquals(70, Delays.randomDelay(70, 70));
		assertEquals(0, Delays.randomDelay(0, 0));
	}

	@Test
	void zeroToOneHundredCoversTheFullSpan() {
		boolean sawZero = false;
		boolean sawHundred = false;
		for (int i = 0; i < 10_000; i++) {
			int delay = Delays.randomDelay(0, 100);
			if (delay == 0) sawZero = true;
			if (delay == 100) sawHundred = true;
		}
		assertTrue(sawZero && sawHundred, "inclusive bounds should both be reachable");
	}
}
