package de.aerialmace.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

public final class HudEditorScreen extends Screen {
    private HudElement dragging;
    private int offsetX;
    private int offsetY;
    public HudEditorScreen() { super(Text.literal("AerialMace HUD Editor")); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);
        context.drawText(textRenderer, "HUD Editor - drag elements, ESC to close", 10, 10, 0xFFFFFFFF, false);
        for (HudElement element : HudManager.elements()) {
            int color = element == dragging ? 0xFF60A5FA : 0xFFFFFFFF;
            context.drawText(textRenderer, element.id(), element.x(), element.y(), color, true);
        }
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        for (HudElement element : HudManager.elements()) {
            if (click.x() >= element.x() && click.x() <= element.x() + 120 && click.y() >= element.y() - 2 && click.y() <= element.y() + 12) {
                dragging = element; offsetX = (int) click.x() - element.x(); offsetY = (int) click.y() - element.y(); return true;
            }
        }
        return true;
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) { dragging.setPosition((int) click.x() - offsetX, (int) click.y() - offsetY); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) { dragging = null; HudManager.save(); return super.mouseReleased(click); }
    @Override public void close() { HudManager.save(); super.close(); }
    @Override public boolean shouldPause() { return false; }
}
