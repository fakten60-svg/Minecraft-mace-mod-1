package de.aerialmace.hud;

import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * One HUD element. HUD elements are deliberately independent of the module system: each one
 * owns its layout (position, scale, visibility) and its own extra settings, which
 * {@link HudManager} persists.
 *
 * <p>Adding a new element means subclassing this class and registering it in
 * {@link HudManager#initialize()} — the editor picks it up automatically.
 */
public abstract class HudElement {

	private final String id;
	private final int defaultX;
	private final int defaultY;
	private final boolean defaultShowLabel;
	private final int defaultDecimals;

	/** Default overlay text color. */
	public static final int DEFAULT_COLOR = 0xFFFFFFFF;

	private int color = DEFAULT_COLOR;
	private int x;
	private int y;
	private boolean visible = true;
	private float scale = 1.0f;
	private boolean showLabel;
	private int decimals;

	protected HudElement(String id, int defaultX, int defaultY) {
		this(id, defaultX, defaultY, true, 1);
	}

	protected HudElement(String id, int defaultX, int defaultY, boolean defaultShowLabel, int defaultDecimals) {
		this.id = id;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.defaultShowLabel = defaultShowLabel;
		this.defaultDecimals = defaultDecimals;
		this.x = defaultX;
		this.y = defaultY;
		this.showLabel = defaultShowLabel;
		this.decimals = defaultDecimals;
	}

	public String id() {
		return id;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void resetPosition() {
		this.x = defaultX;
		this.y = defaultY;
	}

	public boolean visible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void toggleVisible() {
		this.visible = !visible;
	}

	public float scale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = Math.max(0.5f, Math.min(3.0f, scale));
	}

	public boolean showLabel() {
		return showLabel;
	}

	public void setShowLabel(boolean showLabel) {
		this.showLabel = showLabel;
	}

	public void toggleShowLabel() {
		this.showLabel = !showLabel;
	}

	public int decimals() {
		return decimals;
	}

	public void setDecimals(int decimals) {
		this.decimals = Math.max(0, Math.min(3, decimals));
	}

	public int color() {
		return color;
	}

	public void setColor(int color) {
		this.color = color == 0 ? DEFAULT_COLOR : color;
	}

	/** The text this element currently shows (used for rendering and for hitboxes). */
	public abstract String text(MinecraftClient client);

	/** Scaled pixel width at the current scale. */
	public int width(MinecraftClient client) {
		return Math.max(1, Math.round(client.textRenderer.getWidth(text(client)) * scale));
	}

	/** Scaled pixel height at the current scale. */
	public int height(MinecraftClient client) {
		return Math.max(1, Math.round(9 * scale));
	}

	/** Draws the element at its configured position and scale. */
	public final void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		String value = text(client);
		if (value == null || value.isEmpty()) {
			return;
		}
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		context.drawText(client.textRenderer, value, 0, 0, color, true);
		matrices.popMatrix();
	}

	/** True when this element's text can be edited in the HUD editor (e.g. the watermark). */
	public boolean supportsRename() {
		return false;
	}

	/** Current editable text of this element. */
	public String editableText() {
		return "";
	}

	/** Replaces the editable text (blank input falls back to the element's default). */
	public void setEditableText(String value) {
	}

	/** Writes layout plus any element-specific settings. Subclasses extend this. */
	public void writeSettings(JsonObject json) {
		json.addProperty("x", x);
		json.addProperty("y", y);
		json.addProperty("visible", visible);
		json.addProperty("scale", scale);
		json.addProperty("label", showLabel);
		json.addProperty("decimals", decimals);
		json.addProperty("color", color);
	}

	/** Reads layout plus any element-specific settings. Subclasses extend this. */
	public void readSettings(JsonObject json) {
		if (json.has("x") && json.has("y")) {
			setPosition(json.get("x").getAsInt(), json.get("y").getAsInt());
		}
		if (json.has("visible")) {
			setVisible(json.get("visible").getAsBoolean());
		}
		if (json.has("scale")) {
			setScale(json.get("scale").getAsFloat());
		}
		if (json.has("label")) {
			setShowLabel(json.get("label").getAsBoolean());
		}
		if (json.has("decimals")) {
			setDecimals(json.get("decimals").getAsInt());
		}
		if (json.has("color")) {
			setColor(json.get("color").getAsInt());
		}
	}

	/** Restores every default of this element (position, visibility, scale, settings). */
	public void reset() {
		resetPosition();
		visible = true;
		scale = 1.0f;
		showLabel = defaultShowLabel;
		decimals = defaultDecimals;
		color = DEFAULT_COLOR;
	}
}
