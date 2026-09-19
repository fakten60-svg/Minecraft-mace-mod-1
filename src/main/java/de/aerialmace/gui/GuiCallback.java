package de.aerialmace.gui;

/**
 * Callbacks a component can use to talk to the screen without knowing it.
 */
public interface GuiCallback {

	/** Persists config on the next frame. */
	void markDirty();

	/** Plays the UI click sound when enabled in client settings. */
	void playClick();
}
