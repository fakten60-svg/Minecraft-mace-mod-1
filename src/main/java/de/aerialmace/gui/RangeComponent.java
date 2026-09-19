package de.aerialmace.gui;

import de.aerialmace.module.setting.RangeSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Range bar with two independent handles (min/max). Both handles can be dragged, min
 * never crosses max, and every change is written live into the underlying
 * {@link RangeSetting} — so the combat logic uses the new bounds immediately.
 *
 * <pre>
 * 100 ms ●──────────────● 120 ms
 * </pre>
 */
public class RangeComponent extends SettingComponent {

	private final RangeSetting range;
	private boolean draggingMin;
	private boolean draggingMax;

	public RangeComponent(RangeSetting range, GuiCallback callback) {
		super(range, callback);
		this.range = range;
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT + 10;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		var textRenderer = MinecraftClient.getInstance().textRenderer;
		String minText = format(range.getMinValue()) + range.getUnit();
		String maxText = format(range.getMaxValue()) + range.getUnit();

		context.drawText(textRenderer, setting.getName(), x + PADDING, y + 2, theme.text().getColor(), false);

		int barY = y + 13;
		int barX = x + PADDING;
		int barW = width - PADDING * 2;

		int minPos = Math.round(barW * fractionOf(range.getMinValue()));
		int maxPos = Math.round(barW * fractionOf(range.getMaxValue()));

		// Track
		context.fill(barX, barY, barX + barW, barY + 3, 0xFF0B0B0E);
		// Selected range between the two handles
		context.fill(barX + minPos, barY, barX + maxPos, barY + 3, theme.accent().getColor());
		// Two handles
		context.fill(barX + minPos - 2, barY - 2, barX + minPos + 2, barY + 5, theme.text().getColor());
		context.fill(barX + maxPos - 2, barY - 2, barX + maxPos + 2, barY + 5, theme.text().getColor());

		// Values beside the bar (left of / right of the handles, clamped into the row)
		int minX = Math.max(x + PADDING, barX + minPos - textRenderer.getWidth(minText) - 6);
		int maxX = Math.min(x + width - textRenderer.getWidth(maxText) - PADDING, barX + maxPos + 6);
		if (maxX < minX + textRenderer.getWidth(minText)) {
			maxX = x + width - textRenderer.getWidth(maxText) - PADDING;
		}
		context.drawText(textRenderer, minText, minX, y + 2, theme.secondaryText().getColor(), false);
		context.drawText(textRenderer, maxText, maxX, y + 2, theme.secondaryText().getColor(), false);
	}

	private String format(double value) {
		if (range.getStep() >= 1.0) {
			return String.valueOf((int) Math.round(value));
		}
		return String.format("%.2f", value);
	}

	private float fractionOf(double value) {
		return (float) ((value - range.getMin()) / (range.getMax() - range.getMin()));
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		if (click.button() != 0 || !isHovered(mouseX, mouseY, getHeight())) {
			return false;
		}
		int barX = x + PADDING;
		int barW = width - PADDING * 2;
		int minPos = Math.round(barW * fractionOf(range.getMinValue()));
		int maxPos = Math.round(barW * fractionOf(range.getMaxValue()));

		double distMin = Math.abs(mouseX - (barX + minPos));
		double distMax = Math.abs(mouseX - (barX + maxPos));
		// When both handles overlap (the valid 0–0 default for optional delays),
		// choose the side of the shared handle so the user can grow the range in either direction.
		boolean chooseMin = distMin < distMax || (distMin == distMax && mouseX < barX + minPos);
		if (chooseMin) {
			draggingMin = true;
			range.setMinValue(valueFromMouse(mouseX));
		} else {
			draggingMax = true;
			range.setMaxValue(valueFromMouse(mouseX));
		}
		callback.playClick();
		return true;
	}

	@Override
	public void mouseReleased(Click click) {
		if (draggingMin || draggingMax) {
			draggingMin = false;
			draggingMax = false;
			callback.markDirty();
		}
	}

	@Override
	public void mouseDragged(Click click, double deltaX, double deltaY) {
		if (draggingMin) {
			range.setMinValue(valueFromMouse(click.x()));
		} else if (draggingMax) {
			range.setMaxValue(valueFromMouse(click.x()));
		}
	}

	private double valueFromMouse(double mouseX) {
		int barX = x + PADDING;
		int barW = width - PADDING * 2;
		double t = (mouseX - barX) / Math.max(1.0, barW);
		t = Math.max(0.0, Math.min(1.0, t));
		return range.getMin() + t * (range.getMax() - range.getMin());
	}

	@Override
	public boolean isDragging() {
		return draggingMin || draggingMax;
	}
}
