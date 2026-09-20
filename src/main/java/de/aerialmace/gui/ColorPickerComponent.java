package de.aerialmace.gui;

import de.aerialmace.module.setting.ColorSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Simple HSV color picker: click the row to expand an SV square plus hue strip. The
 * chosen color is applied live to the {@link ColorSetting} (and thus the whole GUI
 * theme) and persisted.
 */
public class ColorPickerComponent extends SettingComponent {

	private static final int SWATCH = 8;

	private final ColorSetting color;
	private final Animation openAnim = new Animation(1.4f);
	private boolean open;
	private boolean draggingSV;
	private boolean draggingHue;
	/** Last color the picker knew about; detects changes made outside the picker. */
	private int lastColor;
	private float hue = 0.55f;
	private float saturation = 0.7f;
	private float value = 0.95f;

	public ColorPickerComponent(ColorSetting colorSetting, GuiCallback callback) {
		super(colorSetting, callback);
		this.color = colorSetting;
		this.openAnim.setInstant(false);
		initFromSetting();
	}

	private void initFromSetting() {
		int argb = color.getColor();
		this.lastColor = argb;
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		this.value = max;
		this.saturation = max <= 0 ? 0 : delta / max;
		if (delta <= 0) {
			this.hue = 0;
		} else if (max == r) {
			this.hue = 60 * (((g - b) / delta) % 6);
		} else if (max == g) {
			this.hue = 60 * ((b - r) / delta + 2);
		} else {
			this.hue = 60 * ((r - g) / delta + 4);
		}
		if (hue < 0) {
			hue += 360;
		}
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT + (open ? 62 : 0);
	}

	@Override
	public void update(float deltaSeconds) {
		openAnim.update(deltaSeconds);
		// Re-derive the HSV state when the color changed outside this picker (theme preset,
		// "Reset Theme", config load, cloud config), so the next drag continues from the
		// color that is actually displayed instead of jumping back to a stale one.
		if (!draggingSV && !draggingHue && color.getColor() != lastColor) {
			initFromSetting();
		}
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
		// Current color swatch on the right
		int swx = x + width - SWATCH - PADDING;
		GuiRenderUtil.borderedRect(context, swx, y + (ROW_HEIGHT - SWATCH) / 2, SWATCH, SWATCH,
				color.getColor(), 0xFF000000);

		if (openAnim.value() > 0.01f) {
			int pickerY = y + ROW_HEIGHT;
			int pickerH = (int) (60 * openAnim.eased());
			if (pickerH > 4) {
				int areaX = x + PADDING;
				int areaW = width - PADDING * 2;
				// Never negative while the panel unfolds, otherwise the gradients would be
				// drawn with inverted corners for a frame.
				int squareSize = Math.max(4, pickerH - 12);

				// SV square: white->color horizontal, transparent->black vertical overlay
				int pure = GuiRenderUtil.hsvToRgb(hue, 1, 1) | 0xFF000000;
				GuiRenderUtil.horizontalGradient(context, areaX, pickerY, areaW, squareSize,
						0xFFFFFFFF, pure);
				// Single native gradient instead of one fill per row (cheaper per frame).
				context.fillGradient(areaX, pickerY, areaX + areaW, pickerY + squareSize,
						0x00000000, 0xFF000000);

				// Hue strip below
				GuiRenderUtil.hueStrip(context, areaX, pickerY + squareSize + 4, areaW, 6);

				// Cursor markers
				int cx = areaX + (int) (saturation * areaW);
				int cy = pickerY + (int) ((1 - value) * squareSize);
				context.fill(cx - 2, cy - 1, cx + 2, cy + 1, 0xFFFFFFFF);
				int hx = areaX + (int) (hue / 360.0f * areaW);
				context.fill(hx - 1, pickerY + squareSize + 3, hx + 1, pickerY + squareSize + 11, 0xFFFFFFFF);
			}
		}
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		if (click.button() != 0) {
			return false;
		}
		if (isHovered(mouseX, mouseY, ROW_HEIGHT)) {
			open = !open;
			if (open) {
				openAnim.open();
			} else {
				openAnim.close();
			}
			return true;
		}

		if (!open) {
			return false;
		}
		int areaX = x + PADDING;
		int areaW = width - PADDING * 2;
		int squareSize = 60 - 12;
		int squareY = y + ROW_HEIGHT;
		int hueY = squareY + squareSize + 4;
		int hueH = 6;

		if (mouseY >= squareY && mouseY < squareY + squareSize) {
			draggingSV = true;
			applySV(mouseX, mouseY, areaX, areaW, squareSize, squareY);
			return true;
		}
		if (mouseY >= hueY && mouseY < hueY + hueH) {
			draggingHue = true;
			applyHue(mouseX, areaX, areaW);
			return true;
		}
		return false;
	}

	@Override
	public void mouseReleased(Click click) {
		if (draggingSV || draggingHue) {
			draggingSV = false;
			draggingHue = false;
			callback.markDirty();
		}
	}

	@Override
	public void mouseDragged(Click click, double deltaX, double deltaY) {
		if (draggingSV || draggingHue) {
			int areaX = x + PADDING;
			int areaW = width - PADDING * 2;
			int squareSize = 60 - 12;
			int squareY = y + ROW_HEIGHT;
			if (draggingSV) {
				applySV(click.x(), click.y(), areaX, areaW, squareSize, squareY);
			} else {
				applyHue(click.x(), areaX, areaW);
			}
		}
	}

	private void applySV(double mouseX, double mouseY, int areaX, int areaW, int squareSize, int squareY) {
		saturation = (float) Math.max(0, Math.min(1, (mouseX - areaX) / (float) areaW));
		value = 1.0f - (float) Math.max(0, Math.min(1, (mouseY - squareY) / (float) squareSize));
		apply();
	}

	private void applyHue(double mouseX, int areaX, int areaW) {
		hue = (float) Math.max(0, Math.min(1, (mouseX - areaX) / (float) areaW)) * 360.0f;
		apply();
	}

	private void apply() {
		int rgb = GuiRenderUtil.hsvToRgb(hue, saturation, value);
		// Keep the color's original alpha (theme colors use partial transparency).
		color.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color.getAlpha());
		lastColor = color.getColor();
	}

	@Override
	public boolean keyPressed(int keyCode) {
		return false;
	}

	@Override
	public boolean isDragging() {
		return draggingSV || draggingHue;
	}
}
