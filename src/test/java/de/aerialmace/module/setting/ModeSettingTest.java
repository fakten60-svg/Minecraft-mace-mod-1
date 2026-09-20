package de.aerialmace.module.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Targeted tests for the mode (dropdown) setting: unknown defaults fall back safely,
 * unknown values are rejected and an empty option list must not crash the constructor.
 */
class ModeSettingTest {

	@Test
	void validDefaultIsKept() {
		ModeSetting mode = new ModeSetting("Mode", List.of("Legit", "Rage"), "Rage", v -> {
		});
		assertEquals("Rage", mode.getValue());
	}

	@Test
	void unknownDefaultFallsBackToFirstOption() {
		ModeSetting mode = new ModeSetting("Mode", List.of("Legit", "Rage"), "Nonsense", v -> {
		});
		assertEquals("Legit", mode.getValue());
	}

	@Test
	void unknownValuesAreRejected() {
		ModeSetting mode = new ModeSetting("Mode", List.of("Legit", "Rage"), "Legit", v -> {
		});
		mode.setValue("Injected");
		assertEquals("Legit", mode.getValue(), "values outside the option list must be ignored");
	}

	@Test
	void jsonRoundTrip() {
		ModeSetting mode = new ModeSetting("Mode", List.of("Legit", "Rage"), "Legit", v -> {
		});
		mode.setValue("Rage");
		ModeSetting other = new ModeSetting("Mode", List.of("Legit", "Rage"), "Legit", v -> {
		});
		other.fromJson(mode.toJson());
		assertEquals("Rage", other.getValue());
	}

	@Test
	void fromJsonToleratesGarbage() {
		ModeSetting mode = new ModeSetting("Mode", List.of("Legit", "Rage"), "Legit", v -> {
		});
		mode.fromJson(new com.google.gson.JsonObject());
		assertEquals("Legit", mode.getValue(), "wrong-type input must keep the current value");
		mode.fromJson(null);
		assertEquals("Legit", mode.getValue());
	}

	@Test
	void emptyOptionListDoesNotCrashTheConstructor() {
		assertDoesNotThrow(() -> new ModeSetting("Mode", List.of(), "Legit", v -> {
		}));
		ModeSetting mode = new ModeSetting("Mode", List.of(), "Legit", v -> {
		});
		assertEquals("Legit", mode.getValue());
	}
}
