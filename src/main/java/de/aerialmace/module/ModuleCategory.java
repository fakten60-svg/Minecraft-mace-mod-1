package de.aerialmace.module;

/**
 * GUI categories. Modules are assigned a category once; the ClickGUI builds one panel per
 * category automatically from whatever the {@link ModuleManager} holds.
 */
public enum ModuleCategory {
	COMBAT("Combat"),
	VISUALS("Visuals"),
	MOVEMENT("Movement"),
	MISC("Misc");

	private final String displayName;

	ModuleCategory(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
