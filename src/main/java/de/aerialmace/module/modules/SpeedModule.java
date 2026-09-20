package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;

/**
 * Movement placeholder. Deliberately without settings until a real movement implementation
 * exists - a slider that changes nothing would be a fake setting.
 */
public class SpeedModule extends Module {

	public SpeedModule() {
		super("Speed", "Platzhalter: Bewegungsgeschwindigkeit (noch keine Gameplay-Logik).", ModuleCategory.MOVEMENT);
	}

	@Override
	protected void registerSettings() {
		// No settings on purpose - see the class comment.
	}
}
