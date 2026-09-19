package de.aerialmace.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Reader;
import java.io.Writer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import de.aerialmace.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class HudManager {
    private static final List<HudElement> ELEMENTS = new ArrayList<>();
    private static boolean registered;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aerialmace-hud.json");
    private HudManager() {}

    public static void initialize() {
        if (!ELEMENTS.isEmpty()) return;
        ELEMENTS.add(new TextElement("Watermark", 8, 8, "AerialMace"));
        ELEMENTS.add(new TextElement("FPS", 8, 22, "FPS: ")); 
        ELEMENTS.add(new TextElement("Coordinates", 8, 36, "XYZ: "));
        ELEMENTS.add(new TextElement("Active Modules", 8, 50, "Modules: "));
        load();
        if (!registered) {
            registered = true;
            HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
        }
    }

    public static List<HudElement> elements() { return Collections.unmodifiableList(ELEMENTS); }
    public static HudElement get(String id) { return ELEMENTS.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null); }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            for (HudElement element : ELEMENTS) {
                JsonObject value = new JsonObject(); value.addProperty("x", element.x()); value.addProperty("y", element.y());
                value.addProperty("visible", element.visible()); value.addProperty("scale", element.scale()); root.add(element.id(), value);
            }
            try (Writer writer = Files.newBufferedWriter(FILE)) { GSON.toJson(root, writer); }
        } catch (Exception ignored) { }
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class); if (root == null) return;
            for (HudElement element : ELEMENTS) if (root.has(element.id()) && root.get(element.id()).isJsonObject()) {
                JsonObject value = root.getAsJsonObject(element.id());
                if (value.has("x") && value.has("y")) element.setPosition(value.get("x").getAsInt(), value.get("y").getAsInt());
                if (value.has("visible")) element.setVisible(value.get("visible").getAsBoolean());
                if (value.has("scale")) element.setScale(value.get("scale").getAsFloat());
            }
        } catch (Exception ignored) { }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HudEditorScreen) return;
        for (HudElement element : ELEMENTS) if (element.visible()) element.render(context);
    }

    static final class TextElement extends HudElement {
        private final String prefix;
        TextElement(String id, int x, int y, String prefix) { super(id, x, y); this.prefix = prefix; }
        @Override public void render(DrawContext context) {
            MinecraftClient client = MinecraftClient.getInstance();
            String value = prefix;
            if (id().equals("FPS")) value += client.getCurrentFps();
            else if (id().equals("Coordinates") && client.player != null) {
                value += String.format(java.util.Locale.ROOT, "%.1f / %.1f / %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
            } else if (id().equals("Active Modules")) {
                value += ModuleManager.getModules().stream().filter(m -> m.isEnabled()).map(m -> m.getName()).reduce((a,b) -> a + ", " + b).orElse("None");
            }
            context.drawText(client.textRenderer, value, x(), y(), 0xFFFFFFFF, true);
        }
    }
}
