package de.aerialmace.gui;

import de.aerialmace.module.setting.ActionSetting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;

/**
 * Button row for {@link ActionSetting} (reset functions etc.).
 */
public class SliderActionComponent extends SettingComponent {

	private final ActionSetting action;

	public SliderActionComponent(ActionSetting action, GuiCallback callback) {
		super(action, callback);
		this.action = action;
	}

	@Override
	public int getHeight() {
		return ROW_HEIGHT;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, ThemeManager theme) {
		var textRenderer = MinecraftClient.getInstance().textRenderer;
		boolean hovered = isHovered(mouseX, mouseY, getHeight());
		context.fill(x, y, x + width, y + getHeight(),
				hovered ? ThemeManager.withAlpha(theme.accent().getColor(), 0x30) : 0x00000000);
		String label = action.getDisplayName();
		if (textRenderer.getWidth("↺  " + label) > width - PADDING) {
			label = action.getName();
		}
		context.drawText(textRenderer, "↺  " + label, x + PADDING, y + (ROW_HEIGHT - 8) / 2,
				hovered ? theme.accent().getColor() : theme.secondaryText().getColor(), false);
	}

	@Override
	public boolean mouseClicked(Click click, double mouseX, double mouseY) {
		if (click.button() == 0 && isHovered(mouseX, mouseY, getHeight())) {
			action.run();
			callback.playClick();
			callback.markDirty();
			return true;
		}
		return false;
	}
}
