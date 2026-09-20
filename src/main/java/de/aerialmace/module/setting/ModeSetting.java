package de.aerialmace.module.setting;

import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Mode setting (dropdown in the GUI).
 *
 * <p>The writer is only called on real changes (construction has no side effect).
 */
public class ModeSetting extends Setting {

	private final List<String> options;
	private final String defaultValue;
	private final Consumer<String> writer;
	private String value;

	public ModeSetting(String name, List<String> options, String defaultValue, Consumer<String> writer) {
		super(name);
		this.options = options;
		this.defaultValue = defaultValue;
		this.writer = writer;
		this.value = options.contains(defaultValue) ? defaultValue : options.get(0);
	}

	public List<String> getOptions() {
		return options;
	}

	public String getValue() {
		return value;
	}

	public int getIndex() {
		return options.indexOf(value);
	}

	public void setValue(String newValue) {
		if (options.contains(newValue) && !value.equals(newValue)) {
			value = newValue;
			writer.accept(newValue);
			fireChanged();
		}
	}

	public void cycle() {
		int index = (getIndex() + 1) % options.size();
		setValue(options.get(index));
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
			setValue(element.getAsString());
		}
	}
}
