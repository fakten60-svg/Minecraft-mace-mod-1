package de.aerialmace.module;

import java.util.ArrayList;
import java.util.List;

import de.aerialmace.module.setting.KeybindSetting;
import de.aerialmace.module.setting.Setting;

/**
 * Base class of every client module. A module owns its settings; the ClickGUI only reads
 * and writes them. Modules have NO default keybind ({@link KeybindSetting} starts as NONE)
 * and are toggled through the GUI or a user-assigned key.
 *
 * <p>The existing combat module is adapted to this class via
 * {@link de.aerialmace.module.modules.MaceSwitchModule}, which binds the module system
 * onto {@link de.aerialmace.config.ModConfig} without duplicating any combat logic.
 */
public abstract class Module {

	private final String name;
	private final String description;
	private final ModuleCategory category;
	private final List<Setting> settings = new ArrayList<>();
	private final KeybindSetting keybind;
	private final List<Runnable> toggleListeners = new ArrayList<>();
	private boolean enabled;

	protected Module(String name, String description, ModuleCategory category) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.keybind = new KeybindSetting("Keybind");
		// registerSettings() is NOT called here: subclass fields are not yet assigned.
		// ModuleManager.register() calls finishRegistration() after construction instead.
	}

	/** Called by {@link ModuleManager} once the subclass constructor has fully run. */
	public final void finishRegistration() {
		registerSettings();
		// Every module gets a keybind setting last, always defaulting to NONE.
		settings.add(keybind);
	}

	/** Subclasses add their settings here via {@link #addSetting(Setting)}. */
	protected abstract void registerSettings();

	protected final void addSetting(Setting setting) {
		settings.add(setting);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public ModuleCategory getCategory() {
		return category;
	}

	public List<Setting> getSettings() {
		return settings;
	}

	public KeybindSetting getKeybind() {
		return keybind;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		if (this.enabled != value) {
			this.enabled = value;
			// Persist toggles that happen outside the ClickGUI (keybind, HUD editor, cloud
			// config). The write itself is deferred and coalesced by the config layer.
			de.aerialmace.config.ConfigManager.markDirty();
			onEnabledChanged(value);
			for (Runnable listener : toggleListeners) {
				listener.run();
			}
		}
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void addToggleListener(Runnable listener) {
		toggleListeners.add(listener);
	}

	/** Called by the client tick bridge for enabled modules. */
	public final void tick(net.minecraft.client.MinecraftClient client) {
		if (enabled) onTick(client);
	}

	/** Hook for modules with per-tick client behavior. */
	protected void onTick(net.minecraft.client.MinecraftClient client) {
	}

	/**
	 * Re-reads this module's values from its backing configuration. Called after a config
	 * load, a profile switch or an applied cloud config, so the GUI always shows the values
	 * the actual logic is using (and vice versa). No-op by default.
	 */
	public void refreshFromSource() {
	}

	/** Hook for subclasses; the base class only tracks the flag. */
	protected void onEnabledChanged(boolean nowEnabled) {
	}
}
