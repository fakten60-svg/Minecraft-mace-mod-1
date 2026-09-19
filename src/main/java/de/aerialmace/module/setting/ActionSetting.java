package de.aerialmace.module.setting;

import com.google.gson.JsonElement;

/**
 * A setting that renders as a button in the GUI and runs an action on click
 * (used for the reset functions). Holds no value, so (de)serialization is a no-op.
 */
public class ActionSetting extends Setting {

	private final Runnable action;

	public ActionSetting(String name, Runnable action) {
		super(name);
		this.action = action;
	}

	public void run() {
		action.run();
	}

	@Override
	public void reset() {
		// no value to reset
	}

	@Override
	public JsonElement toJson() {
		return null; // not persisted
	}

	@Override
	public void fromJson(JsonElement element) {
		// nothing to load
	}
}
