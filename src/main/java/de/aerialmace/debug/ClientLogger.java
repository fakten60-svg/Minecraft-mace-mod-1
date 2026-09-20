package de.aerialmace.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aerialmace.AerialMaceClient;

/**
 * Central client logging. All mod logging goes through here so levels are consistent and
 * debug output can be disabled in one place. Never log per tick outside of real problems,
 * and never log sensitive values.
 */
public final class ClientLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(AerialMaceClient.MOD_ID);

	/** Debug logging is opt-in (toggled together with the debug overlay). */
	private static volatile boolean debugEnabled;

	private ClientLogger() {
	}

	public static void setDebugEnabled(boolean enabled) {
		debugEnabled = enabled;
	}

	public static boolean isDebugEnabled() {
		return debugEnabled;
	}

	public static void debug(String message) {
		if (debugEnabled) {
			LOGGER.info("[DEBUG] {}", message);
		}
	}

	public static void info(String message) {
		LOGGER.info(message);
	}

	public static void warn(String message) {
		LOGGER.warn(message);
	}

	public static void warn(String message, Throwable error) {
		LOGGER.warn(message, error);
	}

	public static void error(String message, Throwable error) {
		LOGGER.error(message, error);
	}
}
