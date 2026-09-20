package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;

/**
 * Visuals placeholder. The real HUD elements (watermark, FPS, coordinates, speed, active
 * modules) live in {@link de.aerialmace.hud.HudManager} and are configured in the HUD editor
 * (F8). This module intentionally has no settings: a toggle here would be a second source of
 * truth for the same visibility flags.
 */
public class HUDModule extends Module {

	public HUDModule() {
		super("HUD", "Platzhalter: HUD-Elemente werden im HUD-Editor (F8) verwaltet.", ModuleCategory.VISUALS);
	}

	@Override
	protected void registerSettings() {
		// No settings on purpose - see the class comment.
	}
}
