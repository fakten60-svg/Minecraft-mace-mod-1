package de.aerialmace.gui;

import de.aerialmace.module.setting.Setting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/**
 * Base class of the GUI representation of a {@link Setting}. Components write user input
 * directly into the setting (which forwards to the module logic); they keep no values of
 * their own.
 */
public abstract class SettingComponent {

	protected static final int PADDING = 5;
	protected static final int ROW_HEIGHT = 18;

	protected final Setting setting;
	protected final GuiCallback callback;
	protected int x;
	protected int y;
	protected int width;

	protected SettingComponent(Setting setting, GuiCallback callback) {
		this.setting = setting;
		this.callback = callback;
	}

	public void setBounds(int x, int y, int width) {
		this.x = x;
		this.y = y;
		this.width = width;
	}

	/** The setting this component edits (used for tooltips). */
	public Setting getSetting() {
		return setting;
	}

	/** True when the mouse is over this component's full row. */
	public boolean isHoveredAt(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
	}

	public abstract int getHeight();

	/** Called once per frame with the real delta time (FPS-independent animations). */
	public void update(float deltaSeconds) {
	}

	public abstract void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme);

	/** Returns true when the click was consumed. */
	public abstract boolean mouseClicked(Click click, double mouseX, double mouseY);

	public void mouseReleased(Click click) {
	}

	public void mouseDragged(Click click, double deltaX, double deltaY) {
	}

	/** Returns true when the key was consumed (e.g. keybind recording). */
	public boolean keyPressed(int keyCode) {
		return false;
	}

	/** True while this component holds a drag (so panels skip their own drag logic). */
	public boolean isDragging() {
		return false;
	}

	/**
	 * True while this component captures keyboard input (keybind recording). The screen
	 * checks this so typed characters never leak into the module search or module logic.
	 */
	public boolean isRecording() {
		return false;
	}

	protected boolean isHovered(double mouseX, double mouseY, int height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
}
