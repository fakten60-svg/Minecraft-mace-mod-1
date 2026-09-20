package de.aerialmace.module.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Targeted tests for the keybind setting: modules must never ship a default bind and
 * the GUI keybind must never be clearable to NONE.
 */
class KeybindSettingTest {

	@Test
	void modulesDefaultToNone() {
		KeybindSetting bind = new KeybindSetting("Keybind");
		assertTrue(bind.isNone());
		assertEquals("NONE", bind.getKeyName());
	}

	@Test
	void negativeCodesFallBackToNone() {
		KeybindSetting bind = new KeybindSetting("Keybind");
		bind.setKey(-1);
		assertTrue(bind.isNone());
	}

	@Test
	void guiKeybindDefaultsToRightShift() {
		KeybindSetting bind = new KeybindSetting("GUI Keybind", 344);
		assertEquals(344, bind.getKey());
		assertEquals("Right Shift", bind.getKeyName());
	}

	@Test
	void mouseBindingsRoundTrip() {
		int mouseCode = KeybindSetting.MOUSE_FLAG | 2;
		assertTrue(KeybindSetting.isMouseCode(mouseCode));
		assertEquals(2, KeybindSetting.mouseCodeOf(mouseCode));
		assertFalse(KeybindSetting.isMouseCode(344));
	}

	@Test
	void jsonRoundTrip() {
		KeybindSetting bind = new KeybindSetting("Keybind");
		bind.setKey(87); // W
		KeybindSetting other = new KeybindSetting("Keybind");
		other.fromJson(bind.toJson());
		assertEquals(87, other.getKey());
	}

	@Test
	void fromJsonToleratesGarbage() {
		KeybindSetting bind = new KeybindSetting("Keybind");
		bind.setKey(65);
		bind.fromJson(new com.google.gson.JsonObject()); // wrong type
		assertEquals(65, bind.getKey(), "wrong-type input must keep the current value");
		bind.fromJson(null);
		assertEquals(65, bind.getKey());
	}

	@Test
	void resetRestoresDefault() {
		KeybindSetting bind = new KeybindSetting("Keybind", 344);
		bind.setKey(70);
		bind.reset();
		assertEquals(344, bind.getKey());
	}

	@Test
	void keyNamesCoverImportantKeys() {
		assertEquals("ESC", KeybindSetting.keyName(256));
		assertEquals("F9", KeybindSetting.keyName(298));
		assertEquals("Space", KeybindSetting.keyName(32));
		assertEquals("A", KeybindSetting.keyName(65));
		assertEquals("Key 500", KeybindSetting.keyName(500));
	}
}
