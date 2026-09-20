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
 * Simple JSON configuration for the Gugugaga Client mod.
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
	/** Current config format version; older files keep working (see {@link #load()}). */
	public static final int CONFIG_VERSION = 1;

	/**
	 * The user-facing client name (watermark, window titles, overlay messages). Changed here
	 * so the branding has a single source of truth.
	 */
	public static final String CLIENT_NAME = "Gugugaga Client";

	// ------------------------------------------------------------------
	// Feature toggles
	// ------------------------------------------------------------------

	/**
	 * Master switch (mirrored into the MaceSwitch module's enabled state). A fresh install
	 * starts DISABLED: an automation module must never be armed without the user switching
	 * it on in the ClickGUI or with its keybind. An existing config file keeps its value.
	 */
	public boolean enabled = false;

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

	/** Optional extra settling delay after a target is acquired, before the initial delay. */
	public int targetLockDelayMin = 0;
	public int targetLockDelayMax = 0;

	public int initialDelayMin = 100;
	public int initialDelayMax = 120;
	public int equipToMaceDelayMin = 70;
	public int equipToMaceDelayMax = 80;
	public int maceToAttackDelayMin = 67;
	public int maceToAttackDelayMax = 90;

	/** Optional cooldown after an attack before the machine can return to IDLE. */
	public int postAttackDelayMin = 0;
	public int postAttackDelayMax = 0;

	/** Shows short status messages above the hotbar. */
	public boolean overlayMessages = true;

	// ------------------------------------------------------------------
	// Cloud configs (client config sharing only - never releases).
	// The client never stores private keys; only the public anon key is used.
	// ------------------------------------------------------------------

	/** Supabase project URL used by the shared cloud configs (client config sharing). */
	public String cloudShareUrl = "";
	/** Public Supabase anon key. Safe to ship; protected by row level security policies. */
	public String cloudShareKey = "";
	/** Display name shown next to uploaded cloud configs. */
	public String cloudAuthor = "";

	/** Format version of the file this config was loaded from; rewritten on every save. */
	public int configVersion = CONFIG_VERSION;

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
			// Keep one session backup of the existing file before it may be rewritten.
			Path backup = path.resolveSibling(CONFIG_FILE_NAME + ".bak");
			if (!Files.exists(backup)) {
				ConfigManager.copyToBackup(path, backup);
			}
			try (Reader reader = Files.newBufferedReader(path)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | RuntimeException e) {
				de.aerialmace.debug.ClientLogger.warn("Combat config unreadable; using defaults", e);
				de.aerialmace.notification.NotificationManager.notify(
						de.aerialmace.notification.NotificationType.WARNING, "Config problem",
						"Combat config was reset to defaults");
			}
		}
		if (config.configVersion > CONFIG_VERSION) {
			de.aerialmace.debug.ClientLogger.warn("Combat config written by a newer client version ("
					+ config.configVersion + " > " + CONFIG_VERSION + "); loading with defaults where unknown.");
		}
		// Migration hook for future format changes: transform config here, step by step.
		config.configVersion = CONFIG_VERSION;
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
			// Logged, but the game must keep working even when the config cannot be written.
			de.aerialmace.debug.ClientLogger.warn("Could not save combat config", e);
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

		targetLockDelayMin = clamp(targetLockDelayMin, 0, 10_000);
		targetLockDelayMax = clamp(targetLockDelayMax, targetLockDelayMin, 10_000);
		initialDelayMin = clamp(initialDelayMin, 0, 10_000);
		initialDelayMax = clamp(initialDelayMax, initialDelayMin, 10_000);
		equipToMaceDelayMin = clamp(equipToMaceDelayMin, 0, 10_000);
		equipToMaceDelayMax = clamp(equipToMaceDelayMax, equipToMaceDelayMin, 10_000);
		maceToAttackDelayMin = clamp(maceToAttackDelayMin, 0, 10_000);
		maceToAttackDelayMax = clamp(maceToAttackDelayMax, maceToAttackDelayMin, 10_000);
		postAttackDelayMin = clamp(postAttackDelayMin, 0, 10_000);
		postAttackDelayMax = clamp(postAttackDelayMax, postAttackDelayMin, 10_000);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/** Copies cloud-resolved values into the live config without replacing the object reference. */
	public void copyFrom(ModConfig other) {
		this.enabled = other.enabled;
		this.requireSneaking = other.requireSneaking;
		this.ignoreFriends = other.ignoreFriends;
		this.targetHeightMin = other.targetHeightMin;
		this.targetHeightMax = other.targetHeightMax;
		this.maxTargetDistance = other.maxTargetDistance;
		this.maxHorizontalDistance = other.maxHorizontalDistance;
		this.targetLockDelayMin = other.targetLockDelayMin;
		this.targetLockDelayMax = other.targetLockDelayMax;
		this.initialDelayMin = other.initialDelayMin;
		this.initialDelayMax = other.initialDelayMax;
		this.equipToMaceDelayMin = other.equipToMaceDelayMin;
		this.equipToMaceDelayMax = other.equipToMaceDelayMax;
		this.maceToAttackDelayMin = other.maceToAttackDelayMin;
		this.maceToAttackDelayMax = other.maceToAttackDelayMax;
		this.postAttackDelayMin = other.postAttackDelayMin;
		this.postAttackDelayMax = other.postAttackDelayMax;
		this.overlayMessages = other.overlayMessages;
		this.cloudShareUrl = other.cloudShareUrl;
		this.cloudShareKey = other.cloudShareKey;
		this.cloudAuthor = other.cloudAuthor;
	}

	/**
	 * Snapshot used for cloud uploads. Only gameplay tuning is shared; credentials, remote
	 * endpoints, and the local sharing configuration stay on this machine.
	 */
	public ModConfig sanitizedForSharing() {
		ModConfig copy = new ModConfig();
		copy.copyFrom(this);
		copy.cloudShareUrl = "";
		copy.cloudShareKey = "";
		copy.cloudAuthor = "";
		return copy;
	}
}
