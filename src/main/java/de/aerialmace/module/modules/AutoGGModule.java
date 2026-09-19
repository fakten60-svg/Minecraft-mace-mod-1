package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.BooleanSetting;

/**
 * Simple test module for the Misc category (no real gameplay logic yet).
 */
public class AutoGGModule extends Module {

	public AutoGGModule() {
		super("AutoGG", "Testmodul: schreibt gg nach Spielen (Platzhalter).", ModuleCategory.MISC);
	}

	@Override
	protected void registerSettings() {
		addSetting(new BooleanSetting("Delay", true, value -> {
		}));
	}
}
