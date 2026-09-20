package de.aerialmace.module.setting;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;

/**
 * Base class of every module/client setting. Settings own their value and write changes
 * through to the backing configuration (for the existing combat module that is
 * {@link de.aerialmace.config.ModConfig}) so the GUI never keeps parallel state.
 */
public abstract class Setting {

	private final String name;
	private final List<Runnable> listeners = new ArrayList<>();
	private String description;

	protected Setting(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Short human-readable explanation shown as a tooltip in the GUI. Optional; returns
	 * null for settings without a description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the tooltip text and returns this setting with its concrete type, so module
	 * registrations can chain the call fluently.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Setting> T describe(String description) {
		this.description = description;
		return (T) this;
	}

	public void addListener(Runnable listener) {
		listeners.add(listener);
	}

	protected void fireChanged() {
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	/** Restores the factory default value and fires the change listeners. */
	public abstract void reset();

	/** Serializes the current value for the config file. */
	public abstract JsonElement toJson();

	/** Applies a previously serialized value; tolerant against wrong/missing data. */
	public abstract void fromJson(JsonElement element);
}
