package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;

/**
 * Simple test module for the Movement category (no real gameplay logic yet).
 */
public class SprintModule extends Module {

	public SprintModule() {
		super("Sprint", "Testmodul: dauerhaftes Sprinten (Platzhalter).", ModuleCategory.MOVEMENT);
	}

	@Override
	protected void registerSettings() {
	}
}
