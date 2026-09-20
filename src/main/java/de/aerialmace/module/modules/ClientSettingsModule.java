package de.aerialmace.module.modules;

import java.util.List;

import de.aerialmace.config.ModConfig;
import de.aerialmace.gui.Animation;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.ActionSetting;
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
		public boolean showSearch = true;
		public boolean debugOverlay = false;
		public String theme = "Dark";
	}

	private final State state = new State();
	private final ModConfig combatConfig;

	public ClientSettingsModule() {
		this(null);
	}

	public ClientSettingsModule(ModConfig combatConfig) {
		super("Client Settings",
				"Allgemeine Client-Einstellungen: GUI-Keybind, Skalierung, Animationen und Theme.",
				ModuleCategory.MISC);
		this.combatConfig = combatConfig;
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
				// The ClickGUI has to stay reachable: clearing the bind (NONE) would lock the
				// GUI away for good, so it falls back to the default Right Shift.
				super.setKey(newKey == NONE ? DEFAULT_GUI_KEY : newKey);
				state.guiKey = getKey();
			}
		});

		addSetting(new SliderSetting("GUI Scale", "x", 0.75, 2.0, 1.0, 0.05,
				(value, unused) -> state.guiScale = value))
				.describe("Scales the whole ClickGUI (top-left anchored).");

		addSetting(new SliderSetting("Animation Speed", "x", 0.25, 3.0, 1.0, 0.05,
				(value, unused) -> {
					state.animationSpeed = value;
					Animation.setGlobalSpeed(value.floatValue());
				}))
				.describe("Speed of all GUI and notification animations.");

		addSetting(new BooleanSetting("Blur", true, value -> state.blur = value))
				.describe("Blurs the world behind the ClickGUI.");

		addSetting(new BooleanSetting("Click Sounds", true, value -> state.clickSounds = value))
				.describe("Play a sound on GUI interactions.");

		addSetting(new BooleanSetting("Show Search Bar", true, value -> state.showSearch = value))
				.describe("Show the module search bar inside the ClickGUI.");

		addSetting(new BooleanSetting("Debug Overlay", false, value -> {
			state.debugOverlay = value;
			de.aerialmace.debug.DebugOverlay.setVisible(value);
		})).describe("Diagnostics overlay (also F10): FPS, versions, config state.");

		addSetting(new BooleanSetting("Panel Borders", true,
				value -> de.aerialmace.gui.ThemeManager.get().setShowBorders(value)));

		if (combatConfig != null) {
			// Cloud features are strictly for sharing client configs - never for releases.
			addSetting(new ActionSetting("Open Cloud Configs",
					() -> net.minecraft.client.MinecraftClient.getInstance()
							.setScreen(new de.aerialmace.config.CloudConfigsScreen(combatConfig))));
		}

		addSetting(new ModeSetting("Theme", List.of("Dark", "Midnight", "Neon", "Ocean", "Mono"), "Dark",
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
				tm.border().setColor(0x604F46E5);
				tm.hover().setColor(0x302D1B69);
			}
			case "Neon" -> {
				tm.accent().setColor(0xFF22D3EE);
				tm.background().setColor(0xC0060710);
				tm.panel().setColor(0xE00B1220);
				tm.moduleActive().setColor(0xFF12333D);
				tm.text().setColor(0xFFE6FFFB);
				tm.secondaryText().setColor(0xFF77A9B5);
				tm.border().setColor(0x6034D399);
				tm.hover().setColor(0x3034D399);
			}
			case "Ocean" -> {
				tm.accent().setColor(0xFF38BDF8);
				tm.background().setColor(0xC0071724);
				tm.panel().setColor(0xE00B2233);
				tm.moduleActive().setColor(0xFF123B52);
				tm.text().setColor(0xFFE0F2FE);
				tm.secondaryText().setColor(0xFF7FA8BC);
				tm.border().setColor(0x6040C4FF);
				tm.hover().setColor(0x3038BDF8);
			}
			case "Mono" -> {
				tm.accent().setColor(0xFFE5E5E5);
				tm.background().setColor(0xC0141414);
				tm.panel().setColor(0xE01C1C1C);
				tm.moduleActive().setColor(0xFF303030);
				tm.text().setColor(0xFFF5F5F5);
				tm.secondaryText().setColor(0xFF9C9C9C);
				tm.border().setColor(0x50FFFFFF);
				tm.hover().setColor(0x24FFFFFF);
			}
			default -> {
				tm.accent().setColor(0xFF3B82F6);
				tm.background().setColor(0xC0101014);
				tm.panel().setColor(0xE016161C);
				tm.moduleActive().setColor(0xFF22303F);
				tm.text().setColor(0xFFF2F2F2);
				tm.secondaryText().setColor(0xFF9A9AA5);
				tm.border().setColor(0x503B82F6);
				tm.hover().setColor(0x243B82F6);
			}
		}
	}
}
