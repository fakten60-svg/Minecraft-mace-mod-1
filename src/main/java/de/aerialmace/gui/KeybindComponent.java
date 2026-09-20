package de.aerialmace.gui;

import de.aerialmace.module.setting.KeybindSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Keybind setting row. Left click starts the recording mode ("Press a key...") and the next
 * key is stored. ESC aborts the recording without changing the bind; right-clicking the row
 * clears the bind back to NONE. While recording, a non-primary mouse button (middle, 4, 5)
 * can be assigned as well. All keys are stored as raw GLFW codes, which is exactly what
 * {@link de.aerialmace.input.KeybindManager} polls.
 */
public class KeybindComponent extends SettingComponent {

	private final KeybindSetting keybind;
	private boolean recording;

	public KeybindComponent(KeybindSetting keybind, GuiCallback callback) {
		super(keybind, callback);
		this.keybind = keybind;
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		var textRenderer = MinecraftClient.getInstance().textRenderer;
		boolean hovered = isHovered(mouseX, mouseY, getHeight());
		if (hovered) {
			context.fill(x, y, x + width, y + ROW_HEIGHT, theme.hoverOverlay());
		}

		String valueText = recording ? "Press a key..." : keybind.getKeyName();
		int valueColor = recording ? theme.accent().getColor() : keybind.isNone()
				? theme.secondaryText().getColor()
				: theme.accent().getColor();

		context.drawText(textRenderer, setting.getName(), x + PADDING, y + (ROW_HEIGHT - 8) / 2,
				theme.text().getColor(), false);
		int valueWidth = textRenderer.getWidth(valueText);
		context.drawText(textRenderer, valueText, x + width - valueWidth - PADDING, y + (ROW_HEIGHT - 8) / 2,
				valueColor, false);
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		boolean hovered = isHovered(mouseX, mouseY, getHeight());
		// While "Press a key..." is active, a non-primary mouse button is a bind, not a GUI
		// click. The primary button keeps starting/stopping the recording so the row stays
		// usable as a normal GUI element.
		if (recording && hovered && click.button() > 1) {
			keybind.setKey(KeybindSetting.MOUSE_FLAG | click.button());
			recording = false;
			callback.markDirty();
			callback.playClick();
			return true;
		}
		if (!hovered) {
			if (recording) {
				recording = false;
			}
			return false;
		}
		if (click.button() == 0) {
			recording = true;
			callback.playClick();
			return true;
		}
		if (click.button() == 1) {
			// Right-click clears the bind back to NONE.
			recording = false;
			keybind.setKey(KeybindSetting.NONE);
			callback.markDirty();
			callback.playClick();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode) {
		if (!recording) {
			return false;
		}
		if (keyCode == 256) { // ESC aborts the capture and keeps the previous bind
			recording = false;
			return true;
		}
		keybind.setKey(keyCode);
		recording = false;
		callback.markDirty();
		callback.playClick();
		return true; // consume everything while recording
	}

	/** Ends recording without changing the bind (click landed elsewhere). */
	public void cancelRecording() {
		recording = false;
	}

	/** True while waiting for the next key ("Press a key..."). */
	@Override
	public boolean isRecording() {
		return recording;
	}

	@Override
	public boolean isDragging() {
		return false;
	}
}
