package de.aerialmace.hud;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import de.aerialmace.config.ModConfig;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleManager;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Owns every HUD element and persists their layout and settings in
 * {@code config/aerialmace-hud.json}.
 *
 * <p>Elements are rendered once per frame on the client thread; a corrupt config file only
 * causes the affected element to keep its defaults.
 */
public final class HudManager {

	private static final List<HudElement> ELEMENTS = new ArrayList<>();
	private static boolean registered;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aerialmace-hud.json");

	private HudManager() {
	}

	public static void initialize() {
		if (!ELEMENTS.isEmpty()) {
			return;
		}
		ELEMENTS.add(new WatermarkElement(8, 8));
		ELEMENTS.add(new FpsElement(8, 24));
		ELEMENTS.add(new CoordinatesElement(8, 40));
		ELEMENTS.add(new SpeedElement(8, 56));
		ELEMENTS.add(new ActiveModulesElement(8, 72));
		load();
		if (!registered) {
			registered = true;
			// Current Fabric HUD API (HudRenderCallback is deprecated). addLast renders the
			// overlay after the vanilla layers.
			HudElementRegistry.addLast(Identifier.of("aerialmace", "hud"), (context, tickCounter) -> render(context));
		}
	}

	public static List<HudElement> elements() {
		return Collections.unmodifiableList(ELEMENTS);
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			de.aerialmace.config.ConfigManager.copyToBackup(FILE, FILE.resolveSibling(FILE.getFileName() + ".bak"));
			JsonObject root = new JsonObject();
			for (HudElement element : ELEMENTS) {
				JsonObject value = new JsonObject();
				element.writeSettings(value);
				root.add(element.id(), value);
			}
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException ignored) {
			// HUD layout is non critical; the game keeps running with in-memory values.
		}
	}

	private static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null) {
				return;
			}
			for (HudElement element : ELEMENTS) {
				if (root.has(element.id()) && root.get(element.id()).isJsonObject()) {
					try {
						element.readSettings(root.getAsJsonObject(element.id()));
					} catch (RuntimeException ignored) {
						// Keep this element's defaults when its entry is broken.
					}
				}
			}
		} catch (IOException | RuntimeException ignored) {
			// Unreadable file: keep defaults.
		}
	}

	/** Moves every element back to its default spot ("Reset GUI Layout"). */
	public static void resetLayout() {
		for (HudElement element : ELEMENTS) {
			element.resetPosition();
		}
		save();
	}

	/** Restores positions, visibility, scale and settings of every element. */
	public static void resetAll() {
		for (HudElement element : ELEMENTS) {
			element.reset();
		}
		save();
	}

	/** Draws the live overlay; hidden while the HUD editor is open. */
	public static void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof HudEditorScreen) {
			return;
		}
		for (HudElement element : ELEMENTS) {
			if (element.visible()) {
				element.render(context);
			}
		}
	}

	/** Comma separated list of enabled modules, sorted by name for a stable overlay. */
	private static String activeModules() {
		String modules = ModuleManager.getModules().stream()
				.filter(Module::isEnabled)
				.map(Module::getName)
				.sorted()
				.collect(Collectors.joining(", "));
		return modules.isEmpty() ? "None" : modules;
	}

	// ------------------------------------------------------------------
	// Elements
	// ------------------------------------------------------------------

	/** Shared "Label: value" rendering, honors the label toggle and decimals setting. */
	private abstract static class LabeledElement extends HudElement {

		protected LabeledElement(String id, int x, int y, boolean showLabel, int decimals) {
			super(id, x, y, showLabel, decimals);
		}

		protected abstract String label();

		protected abstract String value(MinecraftClient client);

		@Override
		public String text(MinecraftClient client) {
			String value = value(client);
			return showLabel() ? label() + ": " + value : value;
		}

		protected String format(double value) {
			return String.format(Locale.ROOT, "%." + decimals() + "f", value);
		}
	}

	/** Client name / watermark. Its text can be renamed in the HUD editor. */
	static final class WatermarkElement extends LabeledElement {

		private String text = ModConfig.CLIENT_NAME;

		WatermarkElement(int x, int y) {
			super("Watermark", x, y, false, 1);
		}

		@Override
		protected String label() {
			return "Watermark";
		}

		@Override
		protected String value(MinecraftClient client) {
			return text;
		}

		@Override
		public boolean supportsRename() {
			return true;
		}

		@Override
		public String editableText() {
			return text;
		}

		@Override
		public void setEditableText(String value) {
			String trimmed = value == null ? "" : value.trim();
			this.text = trimmed.isEmpty() ? ModConfig.CLIENT_NAME : trimmed.substring(0, Math.min(32, trimmed.length()));
		}

		@Override
		public void writeSettings(JsonObject json) {
			super.writeSettings(json);
			json.addProperty("text", text);
		}

		@Override
		public void readSettings(JsonObject json) {
			super.readSettings(json);
			if (json.has("text")) {
				setEditableText(json.get("text").getAsString());
			}
		}

		@Override
		public void reset() {
			super.reset();
			text = ModConfig.CLIENT_NAME;
		}
	}

	/** Current frames per second. */
	static final class FpsElement extends LabeledElement {

		FpsElement(int x, int y) {
			super("FPS", x, y, true, 0);
		}

		@Override
		protected String label() {
			return "FPS";
		}

		@Override
		protected String value(MinecraftClient client) {
			return Integer.toString(client.getCurrentFps());
		}
	}

	/** Player coordinates with configurable decimal places. */
	static final class CoordinatesElement extends LabeledElement {

		CoordinatesElement(int x, int y) {
			super("Coordinates", x, y, true, 1);
		}

		@Override
		protected String label() {
			return "XYZ";
		}

		@Override
		protected String value(MinecraftClient client) {
			if (client.player == null) {
				return "-";
			}
			return format(client.player.getX()) + " / " + format(client.player.getY()) + " / " + format(client.player.getZ());
		}
	}

	/** Horizontal movement speed, either in blocks per second or km/h. */
	static final class SpeedElement extends LabeledElement {

		private boolean kilometersPerHour;

		SpeedElement(int x, int y) {
			super("Speed", x, y, true, 1);
		}

		@Override
		protected String label() {
			return "Speed";
		}

		@Override
		protected String value(MinecraftClient client) {
			if (client.player == null) {
				return "-";
			}
			double dx = client.player.getX() - client.player.lastX;
			double dz = client.player.getZ() - client.player.lastZ;
			double blocksPerSecond = Math.sqrt(dx * dx + dz * dz) * 20.0;
			return kilometersPerHour
					? format(blocksPerSecond * 3.6) + " km/h"
					: format(blocksPerSecond) + " b/s";
		}

		void setKilometersPerHour(boolean kilometersPerHour) {
			this.kilometersPerHour = kilometersPerHour;
		}

		boolean isKilometersPerHour() {
			return kilometersPerHour;
		}

		@Override
		public void writeSettings(JsonObject json) {
			super.writeSettings(json);
			json.addProperty("kmh", kilometersPerHour);
		}

		@Override
		public void readSettings(JsonObject json) {
			super.readSettings(json);
			if (json.has("kmh")) {
				kilometersPerHour = json.get("kmh").getAsBoolean();
			}
		}

		@Override
		public void reset() {
			super.reset();
			kilometersPerHour = false;
		}
	}

	/** Enabled modules only, sorted by name. */
	static final class ActiveModulesElement extends LabeledElement {

		ActiveModulesElement(int x, int y) {
			super("Active Modules", x, y, true, 0);
		}

		@Override
		protected String label() {
			return "Modules";
		}

		@Override
		protected String value(MinecraftClient client) {
			return activeModules();
		}
	}
}
