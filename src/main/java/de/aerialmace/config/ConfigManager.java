package de.aerialmace.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.aerialmace.gui.ThemeManager;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleManager;
import de.aerialmace.module.modules.ClientSettingsModule;
import de.aerialmace.module.setting.Setting;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Persistent storage for everything the GUI owns: module enabled states, module settings,
 * module keybinds, the GUI keybind, panel positions, GUI scale, animation speed, blur,
 * click sounds, theme and colors. Loaded on client start, saved on every change.
 *
 * <p>The combat config itself stays in {@link ModConfig}; this file only stores the GUI
 * layer's mirror values (enabled flag, keybinds) and values that are GUI-only.
 */
public final class ConfigManager {

	public static final String CONFIG_FILE_NAME = "aerialmace-gui.json";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigManager() {
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	/** Loads everything; missing entries keep their current (default) values. */
	public static void load(Map<String, double[]> panelPositions) {
		Path path = configPath();
		if (!Files.exists(path)) {
			return;
		}
		JsonObject root;
		try (Reader reader = Files.newBufferedReader(path)) {
			root = GSON.fromJson(reader, JsonObject.class);
		} catch (IOException | RuntimeException e) {
			return;
		}
		if (root == null) {
			return;
		}

		if (root.has("modules") && root.get("modules").isJsonObject()) {
			JsonObject modules = root.getAsJsonObject("modules");
			for (Module module : ModuleManager.getModules()) {
				if (!modules.has(module.getName()) || !modules.get(module.getName()).isJsonObject()) {
					continue;
				}
				JsonObject moduleJson = modules.getAsJsonObject(module.getName());
				if (moduleJson.has("enabled")) {
					module.setEnabled(moduleJson.get("enabled").getAsBoolean());
				}
				if (moduleJson.has("keybind")) {
					module.getKeybind().fromJson(moduleJson.get("keybind"));
				}
				if (moduleJson.has("settings") && moduleJson.get("settings").isJsonObject()) {
					JsonObject settings = moduleJson.getAsJsonObject("settings");
					for (Setting setting : module.getSettings()) {
						if (setting == module.getKeybind()) {
							continue; // already applied above
						}
						if (settings.has(setting.getName())) {
							setting.fromJson(settings.get(setting.getName()));
						}
					}
				}
			}
		}

		if (root.has("panels")) {
			storePanelPositions(root);
		}

		if (root.has("theme") && root.get("theme").isJsonObject()) {
			JsonObject theme = root.getAsJsonObject("theme");
			applyIfPresent(theme, "accent", ThemeManager.get().accent());
			applyIfPresent(theme, "background", ThemeManager.get().background());
			applyIfPresent(theme, "panel", ThemeManager.get().panel());
			applyIfPresent(theme, "moduleActive", ThemeManager.get().moduleActive());
			applyIfPresent(theme, "text", ThemeManager.get().text());
			applyIfPresent(theme, "secondaryText", ThemeManager.get().secondaryText());
		}
	}

	/** Saves everything. Failures are swallowed: config is non critical. */
	public static void save(Map<String, double[]> panelPositions) {
		JsonObject root = new JsonObject();

		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.getModules()) {
			JsonObject moduleJson = new JsonObject();
			moduleJson.addProperty("enabled", module.isEnabled());
			moduleJson.add("keybind", module.getKeybind().toJson());
			JsonObject settings = new JsonObject();
			for (Setting setting : module.getSettings()) {
				if (setting == module.getKeybind()) {
					continue;
				}
				settings.add(setting.getName(), setting.toJson());
			}
			moduleJson.add("settings", settings);
			modules.add(module.getName(), moduleJson);
		}
		root.add("modules", modules);

		JsonObject panels = new JsonObject();
		for (Map.Entry<String, double[]> entry : panelPositions.entrySet()) {
			com.google.gson.JsonArray pos = new com.google.gson.JsonArray();
			pos.add(entry.getValue()[0]);
			pos.add(entry.getValue()[1]);
			panels.add(entry.getKey(), pos);
		}
		root.add("panels", panels);

		JsonObject theme = new JsonObject();
		theme.add("accent", ThemeManager.get().accent().toJson());
		theme.add("background", ThemeManager.get().background().toJson());
		theme.add("panel", ThemeManager.get().panel().toJson());
		theme.add("moduleActive", ThemeManager.get().moduleActive().toJson());
		theme.add("text", ThemeManager.get().text().toJson());
		theme.add("secondaryText", ThemeManager.get().secondaryText().toJson());
		root.add("theme", theme);

		try {
			Files.createDirectories(configPath().getParent());
			try (Writer writer = Files.newBufferedWriter(configPath())) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			// ignore
		}
	}

	private static void applyIfPresent(JsonObject object, String key, Setting setting) {
		if (object.has(key)) {
			setting.fromJson(object.get(key));
		}
	}

	/** Convenience for the client bootstrap: fresh position map. */
	public static Map<String, double[]> newPanelPositionMap() {
		return new HashMap<>();
	}

	// ------------------------------------------------------------------
	// Panel position store (panels read/write their saved spot here).
	// ------------------------------------------------------------------

	private static final Map<String, double[]> PANEL_POSITIONS = new HashMap<>();

	public static double[] getPanelPosition(String key) {
		return PANEL_POSITIONS.get(key);
	}

	public static void setPanelPosition(String key, double[] position) {
		PANEL_POSITIONS.put(key, position);
	}

	/** Reads panel positions from the just-loaded JSON into the store. */
	static void storePanelPositions(JsonObject root) {
		if (root.has("panels") && root.get("panels").isJsonObject()) {
			JsonObject panels = root.getAsJsonObject("panels");
			for (Map.Entry<String, JsonElement> entry : panels.entrySet()) {
				if (entry.getValue().isJsonArray() && entry.getValue().getAsJsonArray().size() == 2) {
					PANEL_POSITIONS.put(entry.getKey(), new double[] {
							entry.getValue().getAsJsonArray().get(0).getAsDouble(),
							entry.getValue().getAsJsonArray().get(1).getAsDouble()
					});
				}
			}
		}
	}
}
