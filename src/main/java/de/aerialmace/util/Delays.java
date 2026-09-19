package de.aerialmace.util;

import java.util.random.RandomGenerator;

/**
 * Small timing helper used by the sequence state machine.
 */
public final class Delays {

	private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

	private Delays() {
	}

	/**
	 * Returns a random delay in milliseconds between {@code min} and {@code max} inclusive so
	 * repeated sequences never produce identical timing.
	 *
	 * <p>Arguments may be given in any order; negative values are clamped to {@code 0}.
	 */
	public static int randomDelay(int min, int max) {
		int lo = Math.max(0, Math.min(min, max));
		int hi = Math.max(0, Math.max(min, max));
		if (lo == hi) {
			return lo;
		}
		return lo + RANDOM.nextInt(hi - lo + 1);
	}
}
