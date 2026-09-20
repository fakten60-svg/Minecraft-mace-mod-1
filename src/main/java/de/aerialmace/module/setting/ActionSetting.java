package de.aerialmace.module.setting;

import java.util.function.Supplier;

import com.google.gson.JsonElement;

/**
 * A setting that renders as a button in the GUI and runs an action on click
 * (used for the reset functions). Holds no value, so (de)serialization is a no-op.
 *
 * <p>An optional {@code label} supplier lets the button text react to state, e.g. the
 * confirmation prompt of a destructive reset.
 */
public class ActionSetting extends Setting {

	private final Runnable action;
	private final Supplier<String> label;

	public ActionSetting(String name, Runnable action) {
		this(name, null, action);
	}

	public ActionSetting(String name, Supplier<String> label, Runnable action) {
		super(name);
		this.label = label;
		this.action = action;
	}

	public void run() {
		action.run();
	}

	/** Text shown on the button: either the dynamic label or the setting name. */
	public String getDisplayName() {
		if (label != null) {
			String custom = label.get();
			if (custom != null && !custom.isBlank()) {
				return custom;
			}
		}
		return getName();
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
