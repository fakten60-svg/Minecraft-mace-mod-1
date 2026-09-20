package de.aerialmace.gui;

import de.aerialmace.module.setting.SliderSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Single-value slider. Writes live into the setting while dragging.
 */
public class SliderComponent extends SettingComponent {

	private final SliderSetting slider;
	private boolean dragging;

	public SliderComponent(SliderSetting slider, GuiCallback callback) {
		super(slider, callback);
		this.slider = slider;
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT + 10;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		var textRenderer = MinecraftClient.getInstance().textRenderer;
		String valueText = format(slider.getValue()) + slider.getUnit();
		context.drawText(textRenderer, setting.getName(), x + PADDING, y + 2, theme.text().getColor(), false);
		int valueWidth = textRenderer.getWidth(valueText);
		context.drawText(textRenderer, valueText, x + width - valueWidth - PADDING, y + 2,
				theme.secondaryText().getColor(), false);

		// Bar
		int barY = y + 13;
		int barX = x + PADDING;
		int barW = width - PADDING * 2;
		context.fill(barX, barY, barX + barW, barY + 3, theme.track());
		float fraction = (float) ((slider.getValue() - slider.getMin())
				/ (slider.getMax() - slider.getMin()));
		int fillW = Math.round(barW * fraction);
		context.fill(barX, barY, barX + fillW, barY + 3, theme.accent().getColor());
		// Handle
		int handleX = barX + fillW - 2;
		context.fill(handleX, barY - 2, handleX + 4, barY + 5, theme.text().getColor());
	}

	private String format(double value) {
		if (slider.getStep() >= 1.0) {
			return String.valueOf((int) Math.round(value));
		}
		return String.format("%.2f", value);
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		if (click.button() == 0 && isHovered(mouseX, mouseY, getHeight())) {
			dragging = true;
			applyFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	public void mouseReleased(Click click) {
		if (dragging) {
			dragging = false;
			callback.markDirty();
		}
	}

	@Override
	public void mouseDragged(Click click, double deltaX, double deltaY) {
		if (dragging) {
			applyFromMouse(click.x());
		}
	}

	private void applyFromMouse(double mouseX) {
		int barX = x + PADDING;
		int barW = width - PADDING * 2;
		double t = (mouseX - barX) / Math.max(1.0, barW);
		t = Math.max(0.0, Math.min(1.0, t));
		slider.setValue(slider.getMin() + t * (slider.getMax() - slider.getMin()));
	}

	@Override
	public boolean isDragging() {
		return dragging;
	}
}
