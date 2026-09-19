package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;

/**
 * Simple test module for the Visuals category (no real gameplay logic yet).
 */
public class ESPModule extends Module {

	public ESPModule() {
		super("ESP", "Testmodul: zeigt Spieler durch Waende (Platzhalter).", ModuleCategory.VISUALS);
	}

	@Override
	protected void registerSettings() {
		// test modules ship without extra settings
	}
}
