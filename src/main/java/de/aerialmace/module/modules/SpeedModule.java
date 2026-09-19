package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.SliderSetting;

/**
 * Simple test module for the Movement category (no real gameplay logic yet).
 */
public class SpeedModule extends Module {

	public SpeedModule() {
		super("Speed", "Testmodul: Bewegungsgeschwindigkeit (Platzhalter).", ModuleCategory.MOVEMENT);
	}

	@Override
	protected void registerSettings() {
		addSetting(new SliderSetting("Speed", "x", 1.0, 3.0, 1.5, 0.1, (value, unused) -> {
		}));
	}
}
