package de.aerialmace.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Keybind setting of a module. The default is always {@link #NONE}: modules must never
 * ship a default keybind. The user assigns keys through the GUI's recording mode.
 *
 * <p>For keyboard keys the GLFW key code is stored; for mouse buttons a marker bit is
 * used so both can live in one value.
 */
public class KeybindSetting extends Setting {

	/** Sentinel value: no key assigned. Modules default to this. */
	public static final int NONE = 0;
	/** Marker bit for mouse button bindings. */
	public static final int MOUSE_FLAG = 0x10000000;
	public static final int MOUSE_CODE_MASK = 0x0FFFFFFF;

	private final int defaultKey;
	private int key;

	public KeybindSetting(String name) {
		this(name, NONE);
	}

	/** Only used for the GUI keybind, whose default is RIGHT_SHIFT. Modules always use NONE. */
	public KeybindSetting(String name, int defaultKey) {
		super(name);
		this.defaultKey = defaultKey;
		this.key = defaultKey;
	}

	public int getKey() {
		return key;
	}

	public boolean isNone() {
		return key == NONE;
	}

	/** Static variant for raw key codes read from config. */
	public static boolean isMouseCode(int code) {
		return code != NONE && (code & MOUSE_FLAG) != 0;
	}

	/** Extracts the pure mouse button code from a stored key value. */
	public static int mouseCodeOf(int code) {
		return code & MOUSE_CODE_MASK;
	}

	public boolean isMouse() {
		return (key & MOUSE_FLAG) != 0;
	}

	public int getMouseCode() {
		return key & MOUSE_CODE_MASK;
	}

	public void setKey(int newKey) {
		if (newKey < 0) {
			newKey = NONE;
		}
		if (key != newKey) {
			key = newKey;
			fireChanged();
		}
	}

	public String getKeyName() {
		return keyName(key);
	}

	public static String keyName(int keyCode) {
		if (keyCode == NONE) {
			return "NONE";
		}
		if ((keyCode & MOUSE_FLAG) != 0) {
			return "Mouse " + (keyCode & MOUSE_CODE_MASK);
		}
		return switch (keyCode) {
			case 256 -> "ESC";
			case 340 -> "Left Shift";
			case 344 -> "Right Shift";
			case 341 -> "Left Ctrl";
			case 345 -> "Right Ctrl";
			case 342 -> "Left Alt";
			case 346 -> "Right Alt";
			case 257 -> "Enter";
			case 259 -> "Backspace";
			case 261 -> "Delete";
			case 262 -> "Right";
			case 263 -> "Left";
			case 264 -> "Down";
			case 265 -> "Up";
			case 32 -> "Space";
			case 335 -> "Keypad Enter";
			case 258 -> "Tab";
			case 260 -> "Insert";
			case 266 -> "Page Up";
			case 267 -> "Page Down";
			case 268 -> "Home";
			case 269 -> "End";
			default -> {
				if (keyCode >= 290 && keyCode <= 301) {
					yield "F" + (keyCode - 289); // F1 - F12
				}
				if (keyCode >= 32 && keyCode <= 126) {
					yield String.valueOf((char) keyCode).toUpperCase();
				}
				yield "Key " + keyCode;
			}
		};
	}

	@Override
	public void reset() {
		setKey(defaultKey);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(key);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			setKey(element.getAsInt());
		}
	}
}
