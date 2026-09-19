package de.aerialmace.gui;

import de.aerialmace.module.setting.KeybindSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Keybind setting row. Click starts the recording mode ("Press a key..."); the next key
 * is stored, ESC or clicking NONE clears the bind back to NONE.
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
		if (click.button() == 0 && isHovered(mouseX, mouseY, getHeight())) {
			recording = true;
			callback.playClick();
			return true;
		}
		// Clicking anywhere else stops recording without changing the bind.
		if (recording) {
			recording = false;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode) {
		if (!recording) {
			return false;
		}
		if (keyCode == 256) { // ESC clears back to NONE
			keybind.setKey(KeybindSetting.NONE);
		} else {
			keybind.setKey(keyCode);
		}
		recording = false;
		callback.markDirty();
		callback.playClick();
		return true; // consume everything while recording
	}

	/** Ends recording without changing the bind (click landed elsewhere). */
	public void cancelRecording() {
		recording = false;
	}

	@Override
	public boolean isDragging() {
		return false;
	}
}
