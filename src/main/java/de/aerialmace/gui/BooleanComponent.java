package de.aerialmace.gui;

import de.aerialmace.module.setting.BooleanSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/**
 * Boolean setting row with an animated ON/OFF toggle at the right edge.
 */
public class BooleanComponent extends SettingComponent {

	private final BooleanSetting booleanSetting;
	private final Animation toggleAnim = new Animation(1.6f);

	public BooleanComponent(BooleanSetting setting, GuiCallback callback) {
		super(setting, callback);
		this.booleanSetting = setting;
		toggleAnim.setInstant(setting.getValue());
		toggleAnim.open();
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		boolean hovered = isHovered(mouseX, mouseY, getHeight());
		var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		context.drawText(textRenderer, setting.getName(), x + PADDING,
				y + (ROW_HEIGHT - 8) / 2, theme.text().getColor(), false);

		// Toggle track + knob (smooth; animated in update())
		int toggleW = 20;
		int toggleH = 9;
		int tx = x + width - toggleW - PADDING;
		int ty = y + (ROW_HEIGHT - toggleH) / 2;
		int fill = ThemeManager.lerp(0xFF2A2A33, theme.accent().getColor(), toggleAnim.eased());
		context.fill(tx, ty, tx + toggleW, ty + toggleH, ThemeManager.withAlpha(fill, 0x66));
		int knob = ThemeManager.lerp(theme.text().getColor(), theme.accent().getColor(), 0.25f);
		int knobPos = tx + 1 + Math.round((toggleW - toggleH) * toggleAnim.eased());
		context.fill(knobPos, ty + 1, knobPos + toggleH - 2, ty + toggleH - 1, knob);
	}

	@Override
	public void update(float deltaSeconds) {
		toggleAnim.update(deltaSeconds);
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		if (click.button() == 0 && isHovered(mouseX, mouseY, getHeight())) {
			booleanSetting.toggle();
			if (booleanSetting.getValue()) {
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
		return false;
	}

	@Override
	public boolean isDragging() {
		return false;
	}
}
