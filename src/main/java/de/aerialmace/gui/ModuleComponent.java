package de.aerialmace.gui;

import java.util.ArrayList;
import java.util.List;

import de.aerialmace.module.Module;
import de.aerialmace.module.setting.Setting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * One module row inside a panel. Left click toggles the module ON/OFF (animated),
 * right click expands/collapses the module's real settings (each rendered by the
 * matching {@link SettingComponent} — including the Keybind row, which defaults to NONE).
 */
public class ModuleComponent {

	private static final int HEADER_HEIGHT = 16;

	private final Module module;
	private final GuiCallback callback;
	private final List<SettingComponent> settingComponents = new ArrayList<>();
	private final Animation expandAnim = new Animation(1.5f);
	private final Animation toggleAnim = new Animation(1.8f);
	private boolean settingsOpen;
	private int x;
	private int y;
	private int width;

	public ModuleComponent(Module module, GuiCallback callback) {
		this.module = module;
		this.callback = callback;
		this.toggleAnim.setInstant(module.isEnabled());
		for (Setting setting : module.getSettings()) {
			settingComponents.add(SettingComponents.create(setting, callback));
		}
	}

	public Module getModule() {
		return module;
	}

	public void setPosition(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
		int settingsY = y + HEADER_HEIGHT;
		for (SettingComponent component : settingComponents) {
			component.setBounds(x, settingsY, width);
			settingsY += component.getHeight();
		}
	}

	/** Current rendered height (settings area animates with the expand progress). */
	public int getHeight() {
		return HEADER_HEIGHT + Math.round(getSettingsHeight() * expandAnim.eased());
	}

	private int getSettingsHeight() {
		int height = 0;
		for (SettingComponent component : settingComponents) {
			height += component.getHeight();
		}
		return height;
	}

	public void update(float deltaSeconds) {
		expandAnim.update(deltaSeconds);
		toggleAnim.update(deltaSeconds);
		for (SettingComponent component : settingComponents) {
			component.update(deltaSeconds);
		}
	}

	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		var textRenderer = MinecraftClient.getInstance().textRenderer;
		boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
		if (hovered) {
			context.fill(x, y, x + width, y + HEADER_HEIGHT, theme.hoverOverlay());
		}

		// Active module background tint.
		float eased = toggleAnim.eased();
		if (eased > 0.01f) {
			int activeBg = ThemeManager.lerp(theme.panel().getColor(), theme.moduleActive().getColor(), eased);
			context.fill(x, y, x + width, y + HEADER_HEIGHT, ThemeManager.withAlpha(activeBg, (int) (0xE0 * eased)));
		}

		// Accent bar on the left edge while enabled.
		if (eased > 0.01f) {
			context.fill(x, y, x + 2, y + HEADER_HEIGHT,
					ThemeManager.withAlpha(theme.accent().getColor(), (int) (0xFF * eased)));
		}

		context.drawText(textRenderer, module.getName(), x + 6, y + (HEADER_HEIGHT - 8) / 2,
				theme.text().getColor(), false);

		// ON/OFF chip on the right.
		String state = module.isEnabled() ? "ON" : "OFF";
		int stateWidth = textRenderer.getWidth(state);
		int chipColor = module.isEnabled() ? theme.accent().getColor() : theme.secondaryText().getColor();
		context.drawText(textRenderer, state, x + width - stateWidth - 6, y + (HEADER_HEIGHT - 8) / 2,
				chipColor, false);

		// Settings area (clipped while animating).
		if (expandAnim.value() > 0.01f) {
			int fullHeight = getSettingsHeight();
			int visibleHeight = Math.round(fullHeight * expandAnim.eased());
			context.enableScissor(x, y + HEADER_HEIGHT, x + width, y + HEADER_HEIGHT + visibleHeight + 1);
			context.fill(x, y + HEADER_HEIGHT, x + width, y + HEADER_HEIGHT + fullHeight,
					ThemeManager.withAlpha(0xFF000000, 0x60));
			for (SettingComponent component : settingComponents) {
				component.render(context, mouseX, mouseY, theme);
			}
			context.disableScissor();
		}
	}

	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		// Settings first (they overlay below the header).
		if (settingsOpen || expandAnim.value() > 0.01f) {
			for (SettingComponent component : settingComponents) {
				if (component.mouseClicked(click, mouseX, mouseY)) {
					callback.markDirty();
					return true;
				}
			}
		}

		if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + HEADER_HEIGHT) {
			return false;
		}

		if (click.button() == 0) {
			module.toggle();
			toggleAnim.setInstant(!module.isEnabled());
			if (module.isEnabled()) {
				toggleAnim.setInstant(false);
				toggleAnim.open();
			} else {
				toggleAnim.setInstant(true);
				toggleAnim.close();
			}
			callback.playClick();
			callback.markDirty();
			return true;
		}
		if (click.button() == 1) {
			settingsOpen = !settingsOpen;
			if (settingsOpen) {
				expandAnim.open();
			} else {
				expandAnim.close();
			}
			callback.playClick();
			return true;
		}
		return false;
	}

	public void mouseReleased(Click click) {
		for (SettingComponent component : settingComponents) {
			component.mouseReleased(click);
		}
	}

	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		boolean consumed = false;
		for (SettingComponent component : settingComponents) {
			if (component.isDragging()) {
				component.mouseDragged(click, deltaX, deltaY);
				consumed = true;
			}
		}
		return consumed;
	}

	public boolean keyPressed(int keyCode) {
		for (SettingComponent component : settingComponents) {
			if (component.keyPressed(keyCode)) {
				callback.markDirty();
				return true;
			}
		}
		return false;
	}

	/** True when a component of this module currently holds a drag. */
	public boolean isDragging() {
		for (SettingComponent component : settingComponents) {
			if (component.isDragging()) {
				return true;
			}
		}
		return false;
	}

	/** Stops keybind recording when a click lands outside this module. */
	public void stopRecordingIfOutside(double mouseX, double mouseY) {
		boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
		if (!inside) {
			for (SettingComponent component : settingComponents) {
				if (component instanceof KeybindComponent keybindComponent) {
					keybindComponent.cancelRecording();
				}
			}
		}
	}

}
