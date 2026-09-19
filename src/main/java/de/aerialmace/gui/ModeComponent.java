package de.aerialmace.gui;

import java.util.List;

import de.aerialmace.module.setting.ModeSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Mode setting row: click to open an animated dropdown, pick an option.
 */
public class ModeComponent extends SettingComponent {

	private final ModeSetting mode;
	private final Animation openAnim = new Animation(1.4f);
	private boolean open;

	public ModeComponent(ModeSetting mode, GuiCallback callback) {
		super(mode, callback);
		this.mode = mode;
		this.openAnim.setInstant(false);
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT + (open ? mode.getOptions().size() * ROW_HEIGHT : 0);
	}

	@Override
	public void update(float deltaSeconds) {
		openAnim.update(deltaSeconds);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		var textRenderer = MinecraftClient.getInstance().textRenderer;
		boolean hovered = isHovered(mouseX, mouseY, ROW_HEIGHT);

		if (hovered) {
			context.fill(x, y, x + width, y + ROW_HEIGHT, theme.hoverOverlay());
		}

		context.drawText(textRenderer, setting.getName(), x + PADDING, y + (ROW_HEIGHT - 8) / 2,
				theme.text().getColor(), false);
		String value = mode.getValue();
		int valueW = textRenderer.getWidth(value);
		context.drawText(textRenderer, value, x + width - valueW - PADDING, y + (ROW_HEIGHT - 8) / 2,
				theme.accent().getColor(), false);

		if (openAnim.value() > 0.01f) {
			float eased = openAnim.eased();
			int listHeight = (int) (mode.getOptions().size() * ROW_HEIGHT * eased);
			int listY = y + ROW_HEIGHT;
			context.fill(x, listY, x + width, listY + listHeight, theme.surface());
			context.fill(x, listY, x + width, listY + 1, ThemeManager.withAlpha(0xFFFFFFFF, 0x18));

			List<String> options = mode.getOptions();
			int visible = Math.min(options.size(), (int) (listHeight / (float) ROW_HEIGHT));
			for (int i = 0; i < visible; i++) {
				String option = options.get(i);
				int rowY = listY + i * ROW_HEIGHT;
				boolean selected = option.equals(mode.getValue());
				context.drawText(textRenderer, option, x + PADDING, rowY + (ROW_HEIGHT - 8) / 2,
						selected ? theme.accent().getColor() : theme.secondaryText().getColor(), false);
			}
		}
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		if (click.button() != 0) {
			return false;
		}

		if (open && mouseY > y + ROW_HEIGHT) {
			int index = (int) ((mouseY - (y + ROW_HEIGHT)) / ROW_HEIGHT);
			List<String> options = mode.getOptions();
			if (index >= 0 && index < options.size()) {
				mode.setValue(options.get(index));
				callback.markDirty();
			}
			open = false;
			callback.playClick();
			return true;
		}

		if (isHovered(mouseX, mouseY, ROW_HEIGHT)) {
			open = !open;
			if (open) {
				openAnim.open();
			} else {
				openAnim.close();
			}
			callback.playClick();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode) {
		return false;
	}

	@Override
	public boolean isDragging() {
		return false;
	}
}
