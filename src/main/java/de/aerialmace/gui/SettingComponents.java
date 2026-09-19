package de.aerialmace.gui;

import com.google.gson.JsonElement;

import de.aerialmace.module.setting.ActionSetting;
import de.aerialmace.module.setting.BooleanSetting;
import de.aerialmace.module.setting.ColorSetting;
import de.aerialmace.module.setting.KeybindSetting;
import de.aerialmace.module.setting.ModeSetting;
import de.aerialmace.module.setting.RangeSetting;
import de.aerialmace.module.setting.Setting;
import de.aerialmace.module.setting.SliderSetting;

/**
 * Maps a {@link Setting} to its GUI component. Adding a new setting type automatically
 * renders it in the GUI once registered here — components are never chosen per module.
 */
final class SettingComponents {

	private SettingComponents() {
	}

	static SettingComponent create(Setting setting, GuiCallback callback) {
		if (setting instanceof RangeSetting range) {
			return new RangeComponent(range, callback);
		}
		if (setting instanceof SliderSetting slider) {
			return new SliderComponent(slider, callback);
		}
		if (setting instanceof BooleanSetting bool) {
			return new BooleanComponent(bool, callback);
		}
		if (setting instanceof ModeSetting mode) {
			return new ModeComponent(mode, callback);
		}
		if (setting instanceof KeybindSetting keybind) {
			return new KeybindComponent(keybind, callback);
		}
		if (setting instanceof ColorSetting color) {
			return new ColorPickerComponent(color, callback);
		}
		if (setting instanceof ActionSetting action) {
			return new SliderActionComponent(action, callback);
		}
		// Unknown settings render nothing (still occupy no space).
		return new EmptyComponent();
	}

	/** Zero-height placeholder for unsupported setting types. */
	private static final class EmptyComponent extends SettingComponent {
		private EmptyComponent() {
			super(new Setting("unknown") {
				@Override
				public void reset() {
				}

				@Override
				public JsonElement toJson() {
					return null;
				}

				@Override
				public void fromJson(JsonElement element) {
				}
			}, null);
		}

		@Override
		public int getHeight() {
			return 0;
		}

		@Override
		public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		}

		@Override
		public boolean mouseClicked(net.minecraft.client.gui.Click click, double mouseX, double mouseY) {
			return false;
		}
	}
}
