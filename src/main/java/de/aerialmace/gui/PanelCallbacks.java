package de.aerialmace.gui;

/**
 * Callbacks panels/components get from the screen: dirty marking, click sounds and the
 * reset functions of the client settings.
 */
public interface PanelCallbacks extends GuiCallback {

	/** Resets all module settings to their defaults (keeps enabled/keybinds). */
	void resetModuleSettings();

	/** GUI keybind → RIGHT_SHIFT, all module keybinds → NONE. */
	void resetKeybinds();

	/** Restores the default theme colors. */
	void resetTheme();

	/** Resets all panel positions to the default layout. */
	void resetLayout();

	/** Restores all client-owned state to defaults. */
	void resetEverything();
}
