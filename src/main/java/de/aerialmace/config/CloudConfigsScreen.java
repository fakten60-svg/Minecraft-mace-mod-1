package de.aerialmace.config;

import java.util.List;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Cloud config browser. The player configures the share endpoint once, can upload the current
 * client config under a chosen name, and can load any config other players shared.
 *
 * <p>Networking happens asynchronously in {@link CloudConfigs}; this screen only renders the
 * finished results and never blocks the game.
 */
public final class CloudConfigsScreen extends Screen {

    private static final int ROW_HEIGHT = 18;
    private static final int LIST_TOP = 150;
    private static final int FIELD_WIDTH = 260;

    /** Editable text fields, cycled with TAB. */
    private enum Field {
        SHARE_URL, SHARE_KEY, AUTHOR, UPLOAD_NAME
    }	private final ModConfig config;
	private Field field = Field.UPLOAD_NAME;
	private int refreshCooldown;
	private long selectedId = -1L;
	private String urlBuffer;
	private String keyBuffer;
	private String authorBuffer;
	private String uploadName = "my-config";


    public CloudConfigsScreen(ModConfig config) {
        super(Text.literal("Cloud Configs"));
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        CloudConfigs.refresh(config);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (refreshCooldown > 0) {
            refreshCooldown--;
        }

        context.fill(0, 0, width, height, 0xD0101014);
        context.drawText(textRenderer, "Cloud Configs", 20, 14, 0xFFFFFFFF, true);
        context.drawText(textRenderer, "Upload your client config and load configs shared by others",
                20, 28, 0xFF9A9AA5, false);
        context.drawText(textRenderer, "TAB switch field  ·  ENTER upload  ·  R refresh  ·  click a row to load  ·  ESC close",
                20, 40, 0xFF9A9AA5, false);

        drawField(context, "Share URL", Field.SHARE_URL, config.cloudShareUrl, 58);
        drawField(context, "Anon Key", Field.SHARE_KEY, mask(config.cloudShareKey), 78);
        drawField(context, "Author", Field.AUTHOR, config.cloudAuthor, 98);
        drawField(context, "Upload Name", Field.UPLOAD_NAME, uploadName, 118);

        String statusLine = CloudConfigs.isConfigured(config)
                ? CloudConfigs.status() + " · " + CloudConfigs.message()
                : "Set Share URL + Anon Key to enable cloud configs";
        int statusColor = CloudConfigs.status() == CloudConfigs.Status.ERROR ? 0xFFF87171 : 0xFF60A5FA;
        context.drawText(textRenderer, statusLine, 20, 138, statusColor, false);

        List<CloudConfigs.Entry> entries = CloudConfigs.entries();
        if (entries.isEmpty()) {
            context.drawText(textRenderer, "No shared configs loaded yet.", 24, LIST_TOP, 0xFF9A9AA5, false);
        }
        int y = LIST_TOP;
        for (CloudConfigs.Entry entry : entries) {
            boolean hovered = mouseY >= y - 2 && mouseY <= y + ROW_HEIGHT - 4;
            if (hovered) {
                context.fill(20, y - 2, width - 20, y + ROW_HEIGHT - 4, 0x22FFFFFF);
            }
            int color = entry.id() == selectedId ? 0xFF60A5FA : 0xFFE5E7EB;
            context.drawText(textRenderer, entry.name(), 24, y, color, false);
            context.drawText(textRenderer, entry.author().isEmpty() ? "unknown" : entry.author(), 300, y, 0xFF9A9AA5, false);
            context.drawText(textRenderer, entry.createdAt(), Math.max(320, width - 200), y, 0xFF9A9AA5, false);
            y += ROW_HEIGHT;
        }
    }

    private void drawField(DrawContext context, String label, Field target, String value, int y) {
        boolean active = field == target;
        int border = active ? 0xFF60A5FA : 0xFF2A2A33;
        context.drawText(textRenderer, label, 20, y + 4, 0xFF9A9AA5, false);
        context.fill(120, y, 120 + FIELD_WIDTH, y + 16, 0xFF16161C);
        context.fill(120, y, 121 + FIELD_WIDTH, y + 1, border);
        context.fill(120, y + 15, 121 + FIELD_WIDTH, y + 16, border);
        String shown = value.isEmpty() ? "..." : value;
        context.drawText(textRenderer, shown, 126, y + 4, value.isEmpty() ? 0xFF6B7280 : 0xFFFFFFFF, false);
    }

    private static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 8) {
            return "\u2022".repeat(value.length());
        }
        return value.substring(0, 4) + "\u2022".repeat(6) + value.substring(value.length() - 4);
    }

    @Override
    public boolean charTyped(CharInput event) {
        if (!event.isValidChar()) {
            return super.charTyped(event);
        }
        char chr = (char) event.codepoint();
        if (!Character.isLetterOrDigit(chr) && chr != '_' && chr != '-' && chr != ' ' && chr != '.' && chr != '/' && chr != ':') {
            return super.charTyped(event);
        }
        String current = currentValue();
        if (current.length() < 200) {
            setCurrentValue(current + chr);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        int key = event.getKeycode();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            saveFields();
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            field = Field.values()[(field.ordinal() + 1) % Field.values().length];
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            String current = currentValue();
            if (!current.isEmpty()) {
                setCurrentValue(current.substring(0, current.length() - 1));
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            saveFields();
            CloudConfigs.upload(config, uploadName);
            return true;
        }		if (key == GLFW.GLFW_KEY_R && refreshCooldown <= 0) {

            refreshCooldown = 20;
            CloudConfigs.refresh(config);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return true;
        }
        // Field focus by clicking a row of inputs.
        if (click.x() >= 120 && click.x() <= 120 + FIELD_WIDTH) {
            if (click.y() >= 58 && click.y() < 74) { field = Field.SHARE_URL; return true; }
            if (click.y() >= 78 && click.y() < 94) { field = Field.SHARE_KEY; return true; }
            if (click.y() >= 98 && click.y() < 114) { field = Field.AUTHOR; return true; }
            if (click.y() >= 118 && click.y() < 134) { field = Field.UPLOAD_NAME; return true; }
        }
        if (click.y() >= LIST_TOP - 2) {
            int index = (int) ((click.y() - LIST_TOP + 2) / ROW_HEIGHT);
            List<CloudConfigs.Entry> entries = CloudConfigs.entries();
            if (index >= 0 && index < entries.size()) {
                CloudConfigs.Entry entry = entries.get(index);
                selectedId = entry.id();
                CloudConfigs.download(config, entry.id());
            }
        }
        return true;
    }

    @Override
    public void removed() {
        saveFields();
        CloudConfigs.poll(config);
        super.removed();
    }

    // ------------------------------------------------------------------
    // Field buffering (the upload name is screen-local, the rest is config)
    // ------------------------------------------------------------------

    private String currentValue() {
        return switch (field) {
            case SHARE_URL -> urlBuffer == null ? config.cloudShareUrl : urlBuffer;
            case SHARE_KEY -> keyBuffer == null ? config.cloudShareKey : keyBuffer;
            case AUTHOR -> authorBuffer == null ? config.cloudAuthor : authorBuffer;
            case UPLOAD_NAME -> uploadName;
        };
    }

    private void setCurrentValue(String value) {
        switch (field) {
            case SHARE_URL -> urlBuffer = value;
            case SHARE_KEY -> keyBuffer = value;
            case AUTHOR -> authorBuffer = value;
            case UPLOAD_NAME -> uploadName = value;
        }
    }

    /** Writes the buffered fields into the config and persists them. */
    private void saveFields() {
        boolean changed = false;
        if (urlBuffer != null && !urlBuffer.equals(config.cloudShareUrl)) {
            config.cloudShareUrl = urlBuffer;
            changed = true;
        }
        if (keyBuffer != null && !keyBuffer.equals(config.cloudShareKey)) {
            config.cloudShareKey = keyBuffer;
            changed = true;
        }
        if (authorBuffer != null && !authorBuffer.equals(config.cloudAuthor)) {
            config.cloudAuthor = authorBuffer;
            changed = true;
        }
        if (changed) {
            ModConfig.requestSave(config);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
