package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.SliderSetting;

/**
 * Simple test module for the Visuals category (no real gameplay logic yet).
 */
public class FullbrightModule extends Module {

	public FullbrightModule() {
		super("Fullbright", "Testmodul: maximale Helligkeit (Platzhalter).", ModuleCategory.VISUALS);
	}

	@Override
	protected void registerSettings() {
		addSetting(new SliderSetting("Brightness", "%", 100, 1500, 1500, 10, (value, unused) -> {
		}));
	}
}
