package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.SliderSetting;

/**
 * Simple test module for the Movement category (no real gameplay logic yet).
 */
public class StepModule extends Module {

	public StepModule() {
		super("Step", "Testmodul: hoeheres Treppensteigen (Platzhalter).", ModuleCategory.MOVEMENT);
	}

	@Override
	protected void registerSettings() {
		addSetting(new SliderSetting("Height", "blocks", 0.6, 2.0, 1.0, 0.1, (value, unused) -> {
		}));
	}
}
