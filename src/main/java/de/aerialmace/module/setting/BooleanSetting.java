package de.aerialmace.module.setting;

import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Boolean toggle. An optional {@code writer} forwards changes to the backing
 * configuration so the GUI never keeps parallel state.
 */
public class BooleanSetting extends Setting {

	private final boolean defaultValue;
	private final Consumer<Boolean> writer;
	private boolean value;

	public BooleanSetting(String name, boolean defaultValue) {
		this(name, defaultValue, null);
	}

	public BooleanSetting(String name, boolean defaultValue, Consumer<Boolean> writer) {
		super(name);
		this.defaultValue = defaultValue;
		this.writer = writer;
		this.value = defaultValue;
		if (writer != null) {
			writer.accept(value);
		}
	}

	public boolean getValue() {
		return value;
	}

	public void setValue(boolean value) {
		if (this.value != value) {
			this.value = value;
			if (writer != null) {
				writer.accept(value);
			}
			fireChanged();
		}
	}

	public void toggle() {
		setValue(!value);
	}

	@Override
	public void reset() {
		setValue(defaultValue);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			setValue(element.getAsBoolean());
		}
	}
}
