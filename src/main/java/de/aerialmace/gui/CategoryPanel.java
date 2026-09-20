package de.aerialmace.gui;

import java.util.ArrayList;
import java.util.List;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.ModuleManager;
import de.aerialmace.module.modules.ClientSettingsModule;
import de.aerialmace.module.setting.ActionSetting;
import de.aerialmace.module.setting.ColorSetting;
import de.aerialmace.module.setting.Setting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/**
 * One draggable panel per category. Modules come from the ModuleManager; the special
 * Client Settings panel additionally shows theme color pickers and the reset actions.
 *
 * <p>Supports header drag &amp; drop, per-panel scrolling (content never bleeds into
 * other panels) and click routing into {@link ModuleComponent}s.
 */
public class CategoryPanel {

	private static final int HEADER_HEIGHT = 22;
	private static final int MAX_VISIBLE_HEIGHT = 260;
	private static final int PANEL_WIDTH = 160;

	private final String storageKey;
	private final String title;
	private final int headerColor;
	private final PanelCallbacks callbacks;
	private final List<ModuleComponent> modules = new ArrayList<>();
	private final List<SettingComponent> directComponents = new ArrayList<>();

	private int x;
	private int y;
	private int width = PANEL_WIDTH;
	private int contentHeight;
	private int visibleArea;
	private int maxVisibleHeight = MAX_VISIBLE_HEIGHT;
	private int renderOffsetY;
	private double scrollOffset;
	private double scrollTarget;
	private String filter = "";
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public CategoryPanel(ModuleCategory category, PanelCallbacks callbacks) {
		this.storageKey = "category." + category.name();
		this.title = category.getDisplayName();
		this.headerColor = ClickGuiScreen.keyDefaultCategoryColor(category);
		this.callbacks = callbacks;
		for (Module module : ModuleManager.getByCategory(category)) {
			modules.add(new ModuleComponent(module, callbacks));
		}
	}

	/** Special panel with the Client Settings module, theme colors and reset actions. */
	public static CategoryPanel clientSettingsPanel(PanelCallbacks callbacks) {
		CategoryPanel panel = new CategoryPanel("settings", "Client Settings", 0xFF8B8B98, callbacks);
		ClientSettingsModule settingsModule = ClientSettingsModule.get();
		if (settingsModule != null) {
			panel.modules.add(new ModuleComponent(settingsModule, callbacks));
		}
		ThemeManager theme = ThemeManager.get();
		panel.directComponents.add(new ColorPickerComponent(theme.accent(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.background(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.panel(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.moduleActive(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.text(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.secondaryText(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.border(), callbacks));
		panel.directComponents.add(new ColorPickerComponent(theme.hover(), callbacks));

		panel.directComponents.add(actionComponent("Reset Module Settings", callbacks::resetModuleSettings, callbacks));
		panel.directComponents.add(actionComponent("Reset Keybinds", callbacks::resetKeybinds, callbacks));
		panel.directComponents.add(actionComponent("Reset Theme", callbacks::resetTheme, callbacks));
		panel.directComponents.add(actionComponent("Reset GUI Layout", callbacks::resetLayout, callbacks));
		// Destructive action, so the label changes while the screen waits for confirmation.
		panel.directComponents.add(SettingComponents.create(new ActionSetting("Reset Everything",
				() -> callbacks.isConfirmingReset() ? "Reset Everything? Click again to confirm" : "Reset Everything",
				callbacks::resetEverything), callbacks));
		return panel;
	}

	private static SettingComponent actionComponent(String name, Runnable action, PanelCallbacks callbacks) {
		return SettingComponents.create(new ActionSetting(name, action), callbacks);
	}

	private CategoryPanel(String storageKey, String title, int headerColor, PanelCallbacks callbacks) {
		this.storageKey = storageKey;
		this.title = title;
		this.headerColor = headerColor;
		this.callbacks = callbacks;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public boolean hasSavedPosition() {
		return ConfigManagerBridge.getPanelPosition(storageKey) != null;
	}

	public void applySavedPosition() {
		double[] position = ConfigManagerBridge.getPanelPosition(storageKey);
		if (position != null && position.length == 2) {
			this.x = (int) position[0];
			this.y = (int) position[1];
		}
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Height budget (in UI units) the panel body may use. Set by the screen so a panel never
	 * grows taller than the window - otherwise its scrolled content would be unreachable at
	 * a high GUI scale.
	 */
	public void setMaxVisibleHeight(int value) {
		this.maxVisibleHeight = Math.max(80, value);
	}

	/** Keeps the panel (and with it its drag handle) inside the visible UI area. */
	public void clampPosition(int maxX, int maxY) {
		this.x = Math.max(0, Math.min(this.x, Math.max(0, maxX)));
		this.y = Math.max(0, Math.min(this.y, Math.max(0, maxY)));
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y + renderOffsetY;
	}

	private int totalHeight() {
		int height = 0;
		for (ModuleComponent module : modules) {
			if (!visible(module)) continue;
			height += module.getHeight();
		}
		for (SettingComponent component : directComponents) {
			height += component.getHeight();
		}
		return height;
	}

	public boolean isMouseOverPanel(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width
				&& mouseY >= y + renderOffsetY && mouseY <= y + renderOffsetY + HEADER_HEIGHT + visibleArea;
	}

	/**
	 * The hovered setting for tooltips: expanded module settings first, then the panel's
	 * own components (theme colors, reset actions). Null when nothing is hovered.
	 */
	public Setting findHoveredSetting(double mouseX, double mouseY) {
		if (!isMouseOverPanel(mouseX, mouseY)) {
			return null;
		}
		for (ModuleComponent module : modules) {
			if (!visible(module)) continue;
			Setting setting = module.findHoveredSetting(mouseX, mouseY);
			if (setting != null) {
				return setting;
			}
		}
		for (SettingComponent component : directComponents) {
			if (component.isHoveredAt(mouseX, mouseY)) {
				return component.getSetting();
			}
		}
		return null;
	}

	public void setFilter(String filter) {
		this.filter = filter == null ? "" : filter;
	}

	private boolean visible(ModuleComponent module) {
		return module.matches(filter);
	}

	public void update(float deltaSeconds) {
		for (ModuleComponent module : modules) {
			module.update(deltaSeconds);
		}
		for (SettingComponent component : directComponents) {
			component.update(deltaSeconds);
		}
		// Smooth per-panel scrolling (FPS independent).
		double diff = scrollTarget - scrollOffset;
		if (Math.abs(diff) < 0.3) {
			scrollOffset = scrollTarget;
		} else {
			scrollOffset += diff * Math.min(1.0, deltaSeconds * 14.0);
		}
	}

	public void render(DrawContext context, int mouseX, int mouseY, float deltaSeconds, float appearEased) {
		renderOffsetY = Math.round((1.0f - appearEased) * -14.0f);
		ThemeManager theme = ThemeManager.get();
		var textRenderer = MinecraftClient.getInstance().textRenderer;

		// Layout pass: components get their absolute positions (including scroll).
		contentHeight = totalHeight();
		int bodyBudget = Math.min(MAX_VISIBLE_HEIGHT, maxVisibleHeight - y - HEADER_HEIGHT);
		visibleArea = Math.min(contentHeight, Math.max(80, bodyBudget));
		double maxScroll = Math.max(0, contentHeight - visibleArea);
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

		int panelY = y + renderOffsetY;
		int panelHeight = HEADER_HEIGHT + visibleArea;

		// Panel background + border.
		if (theme.showBorders()) {
			GuiRenderUtil.borderedRect(context, x - 1, panelY - 1, width + 2, panelHeight + 2,
					theme.panel().getColor(), theme.border().getColor());
		} else {
			context.fill(x, panelY, x + width, panelY + panelHeight, theme.panel().getColor());
		}

		// Header.
		int headerFill = ThemeManager.lerp(theme.panel().getColor(), headerColor, 0.22f);
		context.fill(x, panelY, x + width, panelY + HEADER_HEIGHT, headerFill);
		context.fill(x, panelY + HEADER_HEIGHT - 2, x + width, panelY + HEADER_HEIGHT,
				ThemeManager.withAlpha(headerColor, 0x90));
		context.drawText(textRenderer, title, x + 8, panelY + (HEADER_HEIGHT - 8) / 2,
				theme.text().getColor(), false);

		// Body (clipped => per-panel scrolling).
		context.enableScissor(x, panelY + HEADER_HEIGHT, x + width, panelY + panelHeight);
		int contentY = panelY + HEADER_HEIGHT - (int) Math.round(scrollOffset);
		int moduleX = x;
		for (ModuleComponent module : modules) {
			if (!visible(module)) continue;
			module.setPosition(moduleX, contentY, width);
			module.render(context, mouseX, mouseY, theme);
			contentY += module.getHeight();
		}
		for (SettingComponent component : directComponents) {
			component.setBounds(x, contentY, width);
			component.render(context, mouseX, mouseY, theme);
			contentY += component.getHeight();
		}
		context.disableScissor();

	}

	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		// Header drag start.
		if (mouseY >= y + renderOffsetY && mouseY <= y + renderOffsetY + HEADER_HEIGHT
				&& mouseX >= x && mouseX <= x + width) {
			dragging = true;
			dragOffsetX = (int) (mouseX - x);
			dragOffsetY = (int) (mouseY - y);
			return true;
		}

		if (!isMouseOverPanel(mouseX, mouseY)) {
			return false;
		}

		// Route into module rows (toggle/settings) and direct components.
		for (ModuleComponent module : modules) {
			if (!visible(module)) continue;
			if (module.mouseClicked(click, mouseX, mouseY)) {
				return true;
			}
		}
		for (SettingComponent component : directComponents) {
			if (component.mouseClicked(click, mouseX, mouseY)) {
				callbacks.markDirty();
				return true;
			}
		}
		return true; // clicks inside the panel are consumed (avoid panel-behind hits)
	}

	public void mouseReleased(Click click) {
		dragging = false;
		for (ModuleComponent module : modules) {
			if (visible(module)) module.mouseReleased(click);
		}
		for (SettingComponent component : directComponents) {
			component.mouseReleased(click);
		}
	}

	public boolean mouseDragged(Click click, double mouseX, double mouseY, double deltaX, double deltaY) {
		if (dragging) {
			x = (int) mouseX - dragOffsetX;
			y = (int) mouseY - dragOffsetY;
			return true;
		}
		for (ModuleComponent module : modules) {
			if (!visible(module)) continue;
			if (module.isDragging()) {
				module.mouseDragged(click, deltaX, deltaY);
				return true;
			}
		}
		for (SettingComponent component : directComponents) {
			if (component.isDragging()) {
				component.mouseDragged(click, deltaX, deltaY);
				return true;
			}
		}
		return false;
	}

	public void scroll(double amount) {
		scrollTarget -= amount * 22.0;
	}

	/** True while any component of this panel is capturing a keybind. */
	public boolean isRecording() {
		for (ModuleComponent module : modules) {
			if (module.isRecording()) {
				return true;
			}
		}
		for (SettingComponent component : directComponents) {
			if (component.isRecording()) {
				return true;
			}
		}
		return false;
	}

	/** Forwards keys to components (keybind recording). */
	public boolean keyPressed(int keyCode) {
		for (ModuleComponent module : modules) {
			if (!visible(module)) continue;
			if (module.keyPressed(keyCode)) {
				return true;
			}
		}
		for (SettingComponent component : directComponents) {
			if (component.keyPressed(keyCode)) {
				return true;
			}
		}
		return false;
	}

	/** Persists the current position. */
	public void savePosition() {
		ConfigManagerBridge.setPanelPosition(storageKey, new double[] { x, y });
	}

	/** Stops keybind recording for rows the click did not land in. */
	public void stopRecordingIfOutside(double mouseX, double mouseY) {
		for (ModuleComponent module : modules) {
			if (visible(module)) module.stopRecordingIfOutside(mouseX, mouseY);
		}
	}

	/** Minimal bridge so the panel does not depend on the concrete ConfigManager API. */
	static final class ConfigManagerBridge {
		static double[] getPanelPosition(String key) {
			return de.aerialmace.config.ConfigManager.getPanelPosition(key);
		}

		static void setPanelPosition(String key, double[] position) {
			de.aerialmace.config.ConfigManager.setPanelPosition(key, position);
		}
	}
}
