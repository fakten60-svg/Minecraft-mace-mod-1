package de.aerialmace.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

	private static Path profilesPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("aerialmace-profiles");
	}

	private static String safeProfileName(String name) {
		String value = name == null ? "default" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
		return value.isBlank() ? "default" : value.substring(0, Math.min(40, value.length()));
	}

	/** Saves the current complete GUI/module snapshot as a named profile. */
	public static synchronized boolean saveProfile(String name) {
		try {
			Files.createDirectories(profilesPath());
			// Persist the live state first, otherwise the profile would capture whatever was on
			// disk before the most recent change.
			save();
			Files.copy(configPath(), profilesPath().resolve(safeProfileName(name) + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException ignored) { return false; }
	}

	/** Loads a named snapshot and applies it to the live module/settings state. */
	public static synchronized boolean loadProfile(String name) {
		Path profile = profilesPath().resolve(safeProfileName(name) + ".json");
		if (!Files.exists(profile)) return false;
		try {
			Files.copy(profile, configPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			load(newPanelPositionMap());
			return true;
		} catch (IOException | RuntimeException ignored) { return false; }
	}

	public static synchronized boolean deleteProfile(String name) {
		try { return Files.deleteIfExists(profilesPath().resolve(safeProfileName(name) + ".json")); }
		catch (IOException ignored) { return false; }
	}

	public static synchronized List<String> listProfiles() {
		List<String> result = new ArrayList<>();
		if (!Files.isDirectory(profilesPath())) return result;
		try (var files = Files.list(profilesPath())) {
			files.filter(path -> path.getFileName().toString().endsWith(".json"))
					.map(path -> path.getFileName().toString().replaceFirst("\\.json$", ""))
					.sorted().forEach(result::add);
		} catch (IOException ignored) { }
		return result;
	}

	/**
	 * Loads everything; missing entries keep their current (default) values.
	 *
	 * <p>A corrupt, truncated, or hand-edited file must never crash the client: the whole
	 * body is guarded and every single entry is applied in its own try/catch, so one broken
	 * value only skips that value. Unknown keys are ignored and new settings simply keep
	 * their defaults, which keeps older config files working.
	 */
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

		loading = true;
		try {
			loadModules(root);

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
				applyIfPresent(theme, "border", ThemeManager.get().border());
				applyIfPresent(theme, "hover", ThemeManager.get().hover());
			}

			// The combat values live in ModConfig; pull them back into the GUI so the ClickGUI
			// and the real module logic show and use exactly the same numbers after a config
			// load, a profile switch or an applied cloud config.
			refreshModuleValues();
		} catch (RuntimeException e) {
			// A partially loaded config is still usable; defaults fill the gaps.
		} finally {
			loading = false;
		}
	}

	private static void loadModules(JsonObject root) {
		if (!root.has("modules") || !root.get("modules").isJsonObject()) {
			return;
		}
		JsonObject modules = root.getAsJsonObject("modules");
		for (Module module : ModuleManager.getModules()) {
			JsonElement moduleElement = modules.get(module.getName());
			if (moduleElement == null || !moduleElement.isJsonObject()) {
				continue;
			}
			try {
				loadModule(module, moduleElement.getAsJsonObject());
			} catch (RuntimeException e) {
				// Keep the module's current (default) state when its entry is broken.
			}
		}
	}

	private static void loadModule(Module module, JsonObject moduleJson) {
		JsonElement enabled = moduleJson.get("enabled");
		if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
			module.setEnabled(enabled.getAsBoolean());
		}
		JsonElement keybind = moduleJson.get("keybind");
		if (keybind != null) {
			module.getKeybind().fromJson(keybind);
		}
		JsonElement settingsElement = moduleJson.get("settings");
		if (settingsElement == null || !settingsElement.isJsonObject()) {
			return;
		}
		JsonObject settings = settingsElement.getAsJsonObject();
		for (Setting setting : module.getSettings()) {
			if (setting == module.getKeybind()) {
				continue; // already applied above
			}
			JsonElement value = settings.get(setting.getName());
			if (value == null || value.isJsonNull()) {
				continue;
			}
			try {
				setting.fromJson(value);
			} catch (RuntimeException e) {
				// Wrong type in the file: keep the setting's default/current value.
			}
		}
	}

	/** Saves the current state using the internal panel position store. */
	public static void save() {
		save(new HashMap<>(PANEL_POSITIONS));
	}

	/**
	 * Smallest delay between two automatic writes. Changes mark the config dirty and the
	 * tick bridge flushes it later, so neither a burst of GUI edits nor a keybind toggle
	 * can produce per-frame disk I/O.
	 */
	private static final long AUTO_SAVE_DELAY_MS = 1_000L;

	private static boolean dirty;
	private static long dirtyAtMs;
	private static boolean loading;

	/** Marks the persisted state as changed; the flush is delayed and coalesced. */
	public static void markDirty() {
		if (loading) {
			return; // values just read from disk must not trigger a rewrite
		}
		dirty = true;
		dirtyAtMs = System.currentTimeMillis();
	}

	/** Called once per client tick: writes at most once per {@link #AUTO_SAVE_DELAY_MS}. */
	public static void flushPending() {
		if (!dirty || System.currentTimeMillis() - dirtyAtMs < AUTO_SAVE_DELAY_MS) {
			return;
		}
		dirty = false;
		save();
	}

	/** Forces a write, used when the client shuts down. */
	public static void saveNow() {
		dirty = false;
		save();
	}

	/**
	 * Saves everything. Failures are swallowed: config is non critical.
	 *
	 * <p>The panel position store is updated as well, so positions survive a window resize
	 * (which rebuilds the screen from the store) instead of jumping back to the last
	 * position written before the resize.
	 */
	public static void save(Map<String, double[]> panelPositions) {
		JsonObject root = new JsonObject();

		for (Map.Entry<String, double[]> entry : panelPositions.entrySet()) {
			PANEL_POSITIONS.put(entry.getKey(), entry.getValue().clone());
		}

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
		theme.add("border", ThemeManager.get().border().toJson());
		theme.add("hover", ThemeManager.get().hover().toJson());
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

	/**
	 * Re-syncs every module's GUI values from its backing configuration. Called after
	 * loading a config, switching a profile, or applying a cloud config.
	 */
	public static void refreshModuleValues() {
		for (Module module : ModuleManager.getModules()) {
			try {
				module.refreshFromSource();
			} catch (RuntimeException e) {
				// One broken module must not stop the remaining ones from syncing.
			}
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
