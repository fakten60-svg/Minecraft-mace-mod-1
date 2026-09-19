package de.aerialmace.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Simple JSON configuration for the Aerial Mace Automation mod.
 *
 * <p>The file is stored at {@code config/aerialmace.json}. It is created with defaults on the
 * first start and rewritten whenever it is missing or unreadable, so a broken file can never
 * crash the game. Toggling the automation in game persists immediately, so the state survives
 * a restart.
 *
 * <p>All delay ranges are inclusive milliseconds ranges and are exactly configurable as
 * required by the sequence specification.
 */
public final class ModConfig {
	public static final String CONFIG_FILE_NAME = "aerialmace.json";

	// ------------------------------------------------------------------
	// Feature toggles
	// ------------------------------------------------------------------

	/** Master switch; also toggleable in game with the configured key binding. */
	public boolean enabled = true;

	/** When true the sequence only starts while the local player is sneaking. */
	public boolean requireSneaking = false;

	/** Friends are excluded from target selection by default. */
	public boolean ignoreFriends = true;

	// ------------------------------------------------------------------
	// Targeting
	// ------------------------------------------------------------------

	/** Vertical delta (playerY - targetY) that counts as "roughly 3 blocks above". */
	public double targetHeightMin = 2.85;
	public double targetHeightMax = 3.25;

	/** Maximum total (3D) distance to the target player. */
	public double maxTargetDistance = 5.5;

	/** Maximum horizontal distance to the target player. */
	public double maxHorizontalDistance = 4.5;

	// ------------------------------------------------------------------
	// Delay ranges in milliseconds (inclusive). Exactly as specified:
	// initial 100-120, equip -> mace 70-80, mace -> attack 67-90.
	// ------------------------------------------------------------------

	public int initialDelayMin = 100;
	public int initialDelayMax = 120;
	public int equipToMaceDelayMin = 70;
	public int equipToMaceDelayMax = 80;
	public int maceToAttackDelayMin = 67;
	public int maceToAttackDelayMax = 90;

	/** Shows short status messages above the hotbar. */
	public boolean overlayMessages = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig pendingSave;
	private static long lastSaveAt;

	/** Marks config dirty; the client flushes at most four times per second. */
	public static synchronized void requestSave(ModConfig config) {
		pendingSave = config;
	}

	public static synchronized void flushPending() {
		if (pendingSave != null && System.currentTimeMillis() - lastSaveAt >= 250L) {
			ModConfig config = pendingSave;
			pendingSave = null;
			save(config);
		}
	}

	public ModConfig() {
	}

	/** Loads the config from {@code config/aerialmace.json}, recreating it when missing/corrupt. */
	public static ModConfig load() {
		Path path = configPath();
		ModConfig config = new ModConfig();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | RuntimeException e) {
				// Unreadable/corrupt file: fall back to defaults and rewrite it below.
			}
		}
		config.normalize();
		save(config);
		return config;
	}

	/** Persists the given config; failures are swallowed on purpose (config is non critical). */
	public static synchronized void save(ModConfig config) {
		lastSaveAt = System.currentTimeMillis();
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			// Ignore: the game must keep working even when the config cannot be written.
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	/** Fixes swapped ranges and clamps every value into a sane interval. */
	public void normalize() {
		targetHeightMin = clamp(targetHeightMin, 0.0, 16.0);
		targetHeightMax = clamp(targetHeightMax, targetHeightMin, 32.0);
		maxTargetDistance = clamp(maxTargetDistance, 1.0, 64.0);
		maxHorizontalDistance = clamp(maxHorizontalDistance, 0.5, maxTargetDistance);

		initialDelayMin = clamp(initialDelayMin, 0, 10_000);
		initialDelayMax = clamp(initialDelayMax, initialDelayMin, 10_000);
		equipToMaceDelayMin = clamp(equipToMaceDelayMin, 0, 10_000);
		equipToMaceDelayMax = clamp(equipToMaceDelayMax, equipToMaceDelayMin, 10_000);
		maceToAttackDelayMin = clamp(maceToAttackDelayMin, 0, 10_000);
		maceToAttackDelayMax = clamp(maceToAttackDelayMax, maceToAttackDelayMin, 10_000);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
