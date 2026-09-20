package de.aerialmace.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

/**
 * Standalone HUD editor (F8). It is deliberately separate from the ClickGUI: the screen
 * drags HUD elements, while key presses change the selected element's visibility, scale,
 * label, decimals or text. Nothing here touches module input because a screen is open.
 */
public final class HudEditorScreen extends Screen {

	/** Palette cycled through with C (keeps HUD colors central and predictable). */
	private static final int[] COLOR_PALETTE = {
			0xFFFFFFFF, 0xFF60A5FA, 0xFF34D399, 0xFFFBBF24, 0xFFF87171, 0xFFE5E7EB, 0xFF9A9AA5
	};

	private HudElement selected;
	private HudElement dragging;
	private int offsetX;
	private int offsetY;
	private boolean renaming;
	private String renameBuffer = "";

	public HudEditorScreen() {
		super(Text.literal("AerialMace HUD Editor"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		MinecraftClient client = MinecraftClient.getInstance();
		context.fill(0, 0, width, height, 0x88000000);
		context.drawText(textRenderer, "HUD Editor", 10, 10, 0xFFFFFFFF, true);
		context.drawText(textRenderer,
				"Drag = move  ·  V = show/hide  ·  +/- = scale  ·  C = color  ·  L = label  ·  D = decimals  ·  K = unit (Speed)  ·  ENTER = rename  ·  R = reset layout  ·  ESC = close",
				10, 22, 0xFF9AA0A6, false);

		for (HudElement element : HudManager.elements()) {
			int w = element.width(client);
			int h = element.height(client);
			int x = element.x();
			int y = element.y();

			if (element.visible()) {
				element.render(context);
			} else {
				context.drawText(textRenderer, element.text(client) + "  (hidden)", x, y, 0xFF808080, false);
			}

			int border = element == selected ? 0xFF60A5FA : element.visible() ? 0x66FFFFFF : 0x66FF5555;
			outline(context, x - 2, y - 2, w + 4, h + 4, border);
		}

		String info = selected == null
				? "Click an element to select it"
				: "Selected: " + selected.id() + "  ·  scale " + String.format(java.util.Locale.ROOT, "%.1f", selected.scale())
						+ "  ·  " + (selected.visible() ? "visible" : "hidden")
						+ "  ·  decimals " + selected.decimals();
		context.drawText(textRenderer, info, 10, height - 30, 0xFF9AA0A6, false);

		if (renaming && selected != null) {
			String prompt = "Rename: " + renameBuffer + "_";
			context.fill(10, height - 18, 22 + textRenderer.getWidth(prompt), height - 4, 0xCC16161C);
			context.drawText(textRenderer, prompt, 14, height - 14, 0xFF60A5FA, false);
		}
	}

	private static void outline(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + 1, color);
		context.fill(x, y + h - 1, x + w, y + h, color);
		context.fill(x, y, x + 1, y + h, color);
		context.fill(x + w - 1, y, x + w, y + h, color);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() != 0) {
			return true;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		for (HudElement element : HudManager.elements()) {
			int w = element.width(client);
			int h = Math.max(element.height(client), 10);
			if (click.x() >= element.x() - 2 && click.x() <= element.x() + w + 2
					&& click.y() >= element.y() - 2 && click.y() <= element.y() + h + 2) {
				selected = element;
				dragging = element;
				offsetX = (int) click.x() - element.x();
				offsetY = (int) click.y() - element.y();
				renaming = false;
				return true;
			}
		}
		selected = null;
		dragging = null;
		return true; // consume clicks so nothing behind the editor reacts
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		if (dragging == null) {
			return super.mouseDragged(click, deltaX, deltaY);
		}
		int x = (int) click.x() - offsetX;
		int y = (int) click.y() - offsetY;
		// Keep elements on screen so they can always be grabbed again.
		x = Math.max(0, Math.min(Math.max(0, width - dragging.width(MinecraftClient.getInstance())), x));
		y = Math.max(0, Math.min(Math.max(0, height - dragging.height(MinecraftClient.getInstance())), y));
		dragging.setPosition(x, y);
		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (dragging != null) {
			HudManager.save();
		}
		dragging = null;
		return super.mouseReleased(click);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		int key = input.getKeycode();
		if (renaming) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				applyRename();
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				renaming = false;
				return true;
			}
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				if (!renameBuffer.isEmpty()) {
					renameBuffer = renameBuffer.substring(0, renameBuffer.length() - 1);
				}
				return true;
			}
			return true; // swallow keys while renaming
		}

		if (key == GLFW.GLFW_KEY_ESCAPE) {
			HudManager.save();
			close();
			return true;
		}
		if (selected == null) {
			if (key == GLFW.GLFW_KEY_R) {
				HudManager.resetLayout();
				return true;
			}
			return super.keyPressed(input);
		}

		switch (key) {
			case GLFW.GLFW_KEY_V -> {
				selected.toggleVisible();
				HudManager.save();
				return true;
			}
			case GLFW.GLFW_KEY_L -> {
				selected.toggleShowLabel();
				HudManager.save();
				return true;
			}
			case GLFW.GLFW_KEY_D -> {
				selected.setDecimals((selected.decimals() + 1) % 4);
				HudManager.save();
				return true;
			}
			case GLFW.GLFW_KEY_C -> {
				selected.setColor(nextColor(selected.color()));
				HudManager.save();
				return true;
			}
			case GLFW.GLFW_KEY_K -> {
				if (selected instanceof HudManager.SpeedElement speed) {
					speed.setKilometersPerHour(!speed.isKilometersPerHour());
					HudManager.save();
				}
				return true;
			}
			case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
				selected.setScale(selected.scale() + 0.1f);
				HudManager.save();
				return true;
			}
			case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
				selected.setScale(selected.scale() - 0.1f);
				HudManager.save();
				return true;
			}
			case GLFW.GLFW_KEY_R -> {
				HudManager.resetLayout();
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (selected.supportsRename()) {
					renaming = true;
					renameBuffer = selected.editableText();
				}
				return true;
			}
			default -> {
				return super.keyPressed(input);
			}
		}
	}

	private static int nextColor(int current) {
		for (int index = 0; index < COLOR_PALETTE.length; index++) {
			if (COLOR_PALETTE[index] == current) {
				return COLOR_PALETTE[(index + 1) % COLOR_PALETTE.length];
			}
		}
		return COLOR_PALETTE[0];
	}

	private void applyRename() {
		selected.setEditableText(renameBuffer);
		renaming = false;
		HudManager.save();
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!renaming) {
			return super.charTyped(input);
		}
		if (input.isValidChar()) {
			char chr = (char) input.codepoint();
			if ((Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == ' ') && renameBuffer.length() < 32) {
				renameBuffer += chr;
			}
		}
		return true;
	}

	@Override
	public void close() {
		HudManager.save();
		super.close();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
