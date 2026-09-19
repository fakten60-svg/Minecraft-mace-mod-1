package de.aerialmace.hud;

import net.minecraft.client.gui.DrawContext;

public abstract class HudElement {
    private final String id;
    private int x;
    private int y;
    private boolean visible = true;
    private float scale = 1.0f;

    protected HudElement(String id, int x, int y) { this.id = id; this.x = x; this.y = y; }
    public String id() { return id; }
    public int x() { return x; }
    public int y() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public boolean visible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public float scale() { return scale; }
    public void setScale(float scale) { this.scale = Math.max(0.5f, Math.min(3.0f, scale)); }
    public abstract void render(DrawContext context);
}
