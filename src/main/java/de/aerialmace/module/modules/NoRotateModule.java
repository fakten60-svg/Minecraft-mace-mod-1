package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.BooleanSetting;

/**
 * Simple test module for the Misc category (no real gameplay logic yet).
 */
public class NoRotateModule extends Module {

	public NoRotateModule() {
		super("NoRotate", "Testmodul: ignoriert Server-Rotationen (Platzhalter).", ModuleCategory.MISC);
	}

	@Override
	protected void registerSettings() {
		addSetting(new BooleanSetting("Pitch", true, value -> {
		}));
	}
}
