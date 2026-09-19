package de.aerialmace.module.modules;

import java.util.List;

import de.aerialmace.gui.Animation;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.BooleanSetting;
import de.aerialmace.module.setting.KeybindSetting;
import de.aerialmace.module.setting.ModeSetting;
import de.aerialmace.module.setting.SliderSetting;

/**
 * General client settings: GUI keybind (default RIGHT_SHIFT), GUI scale, animation
 * speed, blur, click sounds and theme selection. Also provides global access for the
 * keybind manager and the config layer.
 */
public class ClientSettingsModule extends Module {

	public static final int DEFAULT_GUI_KEY = 344; // Right Shift (GLFW)

	private static ClientSettingsModule instance;

	/** Live state read by the keybind manager and the GUI. */
	public static final class State {
		public int guiKey = DEFAULT_GUI_KEY;
		public double guiScale = 1.0;
		public double animationSpeed = 1.0;
		public boolean blur = true;
		public boolean clickSounds = true;
		public String theme = "Dark";
	}

	private final State state = new State();

	public ClientSettingsModule() {
		super("Client Settings",
				"Allgemeine Client-Einstellungen: GUI-Keybind, Skalierung, Animationen und Theme.",
				ModuleCategory.MISC);
		// registerSettings() runs via ModuleManager.finishRegistration()
		instance = this;
	}

	public static ClientSettingsModule get() {
		return instance;
	}

	public State getState() {
		return state;
	}

	@Override
	protected void registerSettings() {
		// The only keybind WITH a default (Right Shift); module keybinds stay NONE.
		addSetting(new KeybindSetting("GUI Keybind", DEFAULT_GUI_KEY) {
			@Override
			public void setKey(int newKey) {
				super.setKey(newKey);
				state.guiKey = getKey();
			}
		});

		addSetting(new SliderSetting("GUI Scale", "x", 0.75, 2.0, 1.0, 0.05,
				(value, unused) -> state.guiScale = value));

		addSetting(new SliderSetting("Animation Speed", "x", 0.25, 3.0, 1.0, 0.05,
				(value, unused) -> {
					state.animationSpeed = value;
					Animation.setGlobalSpeed(value.floatValue());
				}));

		addSetting(new BooleanSetting("Blur", true, value -> state.blur = value));

		addSetting(new BooleanSetting("Click Sounds", true, value -> state.clickSounds = value));

		addSetting(new ModeSetting("Theme", List.of("Dark", "Midnight", "Mono"), "Dark",
				value -> {
					state.theme = value;
					applyThemePreset(value);
				}));
	}

	/** Applies a named theme preset to the central ThemeManager colors. */
	private static void applyThemePreset(String theme) {
		var tm = de.aerialmace.gui.ThemeManager.get();
		switch (theme) {
			case "Midnight" -> {
				tm.accent().setColor(0xFF8B5CF6);
				tm.background().setColor(0xC0080A14);
				tm.panel().setColor(0xE00D1024);
				tm.moduleActive().setColor(0xFF1D1B36);
				tm.text().setColor(0xFFE4E4F0);
				tm.secondaryText().setColor(0xFF8888A8);
			}
			case "Mono" -> {
				tm.accent().setColor(0xFFE5E5E5);
				tm.background().setColor(0xC0141414);
				tm.panel().setColor(0xE01C1C1C);
				tm.moduleActive().setColor(0xFF303030);
				tm.text().setColor(0xFFF5F5F5);
				tm.secondaryText().setColor(0xFF9C9C9C);
			}
			default -> {
				tm.accent().setColor(0xFF3B82F6);
				tm.background().setColor(0xC0101014);
				tm.panel().setColor(0xE016161C);
				tm.moduleActive().setColor(0xFF22303F);
				tm.text().setColor(0xFFF2F2F2);
				tm.secondaryText().setColor(0xFF9A9AA5);
			}
		}
	}
}
