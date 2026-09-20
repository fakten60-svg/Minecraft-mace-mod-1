package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;

/**
 * Misc placeholder. Deliberately without settings until a real rotation implementation
 * exists - a toggle that changes nothing would be a fake setting.
 */
public class NoRotateModule extends Module {

	public NoRotateModule() {
		super("NoRotate", "Platzhalter: ignoriert Server-Rotationen (noch keine Gameplay-Logik).", ModuleCategory.MISC);
	}

	@Override
	protected void registerSettings() {
		// No settings on purpose - see the class comment.
	}
}
