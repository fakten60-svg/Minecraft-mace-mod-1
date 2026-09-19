package de.aerialmace.module.modules;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.BooleanSetting;

/**
 * Simple test module for the Visuals category (no real gameplay logic yet).
 */
public class HUDModule extends Module {

	public HUDModule() {
		super("HUD", "Testmodul: HUD-Overlay (Platzhalter).", ModuleCategory.VISUALS);
	}

	@Override
	protected void registerSettings() {
		addSetting(new BooleanSetting("Watermark", true, value -> {
		}));
	}
}
