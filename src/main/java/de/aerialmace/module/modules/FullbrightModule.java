package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;

/**
 * Visuals placeholder. Deliberately without settings until a real brightness implementation
 * exists - a slider that changes nothing would be a fake setting.
 */
public class FullbrightModule extends Module {

	public FullbrightModule() {
		super("Fullbright", "Platzhalter: maximale Helligkeit (noch keine Gameplay-Logik).", ModuleCategory.VISUALS);
	}

	@Override
	protected void registerSettings() {
		// No settings on purpose - see the class comment.
	}
}
