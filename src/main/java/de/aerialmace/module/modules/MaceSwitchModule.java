package de.aerialmace.module.modules;

import de.aerialmace.config.ModConfig;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.BooleanSetting;
import de.aerialmace.module.setting.RangeSetting;
import de.aerialmace.module.setting.SliderSetting;

/**
 * GUI/module adapter for the EXISTING aerial mace combat module.
 *
 * <p>This class contains no combat logic of its own. Every setting writes straight into
 * {@link ModConfig}, which is the single source of truth the
 * {@link de.aerialmace.sequence.SequenceStateMachine} reads on every tick — so changes made
 * in the ClickGUI take effect immediately in the real combat logic.
 *
 * <p>{@link ModConfig} is also the only place the combat values are persisted, and
 * {@link #refreshFromSource()} pulls them back into the GUI whenever they change behind its
 * back (startup, profile switch, applied cloud config). The two directions therefore stay in
 * sync: <em>GUI value → ModConfig → combat logic</em> and
 * <em>ModConfig → GUI value</em>.
 *
 * <p>The settings are constructed with the documented factory defaults, so "Reset Module
 * Settings" restores the spec values (initial 100–120, equip 70–80, attack 67–90 ms) instead
 * of whatever happened to be stored at the last launch. The stored values are applied right
 * afterwards by {@link #refreshFromSource()}.
 *
 * <p>The module's enabled flag is mirrored to {@link ModConfig#enabled} so the previous
 * config file keeps working.
 */
public class MaceSwitchModule extends Module {

	// Documented factory defaults (see the sequence specification).
	private static final int DEFAULT_INITIAL_DELAY_MIN = 100;
	private static final int DEFAULT_INITIAL_DELAY_MAX = 120;
	private static final int DEFAULT_EQUIP_DELAY_MIN = 70;
	private static final int DEFAULT_EQUIP_DELAY_MAX = 80;
	private static final int DEFAULT_ATTACK_DELAY_MIN = 67;
	private static final int DEFAULT_ATTACK_DELAY_MAX = 90;
	private static final double DEFAULT_TARGET_HEIGHT = 3.0;
	private static final double DEFAULT_TARGET_TOLERANCE = 0.2;

	private final ModConfig config;

	// Live handles so the GUI can be re-synced from the config at any time.
	private RangeSetting targetLockDelay;
	private RangeSetting initialDelay;
	private RangeSetting equipDelay;
	private RangeSetting attackDelay;
	private RangeSetting postAttackDelay;
	private SliderSetting targetHeight;
	private SliderSetting targetTolerance;
	private SliderSetting maxDistance;
	private SliderSetting maxHorizontalDistance;
	private BooleanSetting ignoreFriends;
	private BooleanSetting requireSneaking;
	private BooleanSetting overlayMessages;

	public MaceSwitchModule(ModConfig config) {
		super("MaceSwitch",
				"Ruestet die Chestplate aus, wechselt zur Mace und greift einen Spieler an, der ca. 3 Bloecke unter dir ist.",
				ModuleCategory.COMBAT);
		this.config = config;
		setEnabled(config.enabled);
	}

	@Override
	protected void registerSettings() {
		// --------------------------------------------------------------
		// Random delay ranges (rendered as two-handle range bars).
		// The optional ranges add target-settling and post-attack cooldown timing.
		// --------------------------------------------------------------
		targetLockDelay = addRange("Target Lock Delay", 0, 500, 0, 0,
				(min, max) -> {
					config.targetLockDelayMin = min;
					config.targetLockDelayMax = max;
				});

		initialDelay = addRange("Initial Delay", 40, 400,
				DEFAULT_INITIAL_DELAY_MIN, DEFAULT_INITIAL_DELAY_MAX,
				(min, max) -> {
					config.initialDelayMin = min;
					config.initialDelayMax = max;
				});

		equipDelay = addRange("Equip Delay", 40, 400,
				DEFAULT_EQUIP_DELAY_MIN, DEFAULT_EQUIP_DELAY_MAX,
				(min, max) -> {
					config.equipToMaceDelayMin = min;
					config.equipToMaceDelayMax = max;
				});

		attackDelay = addRange("Attack Delay", 40, 400,
				DEFAULT_ATTACK_DELAY_MIN, DEFAULT_ATTACK_DELAY_MAX,
				(min, max) -> {
					config.maceToAttackDelayMin = min;
					config.maceToAttackDelayMax = max;
				});

		postAttackDelay = addRange("Post-Attack Cooldown", 0, 500, 0, 0,
				(min, max) -> {
					config.postAttackDelayMin = min;
					config.postAttackDelayMax = max;
				});

		// --------------------------------------------------------------
		// Targeting: height (center) and tolerance both write the two config
		// values the state machine compares against, so they always agree.
		// --------------------------------------------------------------
		targetHeight = addSlider("Target Height", "blocks", 0.5, 16.0, DEFAULT_TARGET_HEIGHT, 0.05,
				value -> {
					// The tolerance is read live: a value captured while the settings were
					// built would make the two sliders overwrite each other.
					double tolerance = currentTolerance();
					config.targetHeightMin = Math.max(0.0, value - tolerance);
					config.targetHeightMax = value + tolerance;
				});

		targetTolerance = addSlider("Target Tolerance", "blocks", 0.05, 2.0, DEFAULT_TARGET_TOLERANCE, 0.05,
				value -> {
					double center = currentTargetHeight();
					config.targetHeightMin = Math.max(0.0, center - value);
					config.targetHeightMax = center + value;
				});

		maxHorizontalDistance = addSlider("Max Horizontal Distance", "blocks", 0.5, 32.0,
				config.maxHorizontalDistance, 0.5,
				value -> config.maxHorizontalDistance = Math.max(0.5, value));

		maxDistance = addSlider("Max Distance", "blocks", 1.0, 32.0, config.maxTargetDistance, 0.5,
				value -> {
					config.maxTargetDistance = value;
					// The horizontal reach can never be larger than the total reach.
					if (config.maxHorizontalDistance > value) {
						config.maxHorizontalDistance = value;
						maxHorizontalDistance.setValue(value);
					}
				});

		// --------------------------------------------------------------
		// Misc behavior switches bound to the existing config fields.
		// --------------------------------------------------------------
		ignoreFriends = addToggle("Ignore Friends", config.ignoreFriends,
				value -> config.ignoreFriends = value);

		requireSneaking = addToggle("Require Sneaking", config.requireSneaking,
				value -> config.requireSneaking = value);

		overlayMessages = addToggle("Overlay Messages", config.overlayMessages,
				value -> config.overlayMessages = value);


		// The stored config values win over the factory defaults.
		refreshFromSource();
	}

	@Override
	public void refreshFromSource() {
		targetLockDelay.setRange(config.targetLockDelayMin, config.targetLockDelayMax);
		initialDelay.setRange(config.initialDelayMin, config.initialDelayMax);
		equipDelay.setRange(config.equipToMaceDelayMin, config.equipToMaceDelayMax);
		attackDelay.setRange(config.maceToAttackDelayMin, config.maceToAttackDelayMax);
		postAttackDelay.setRange(config.postAttackDelayMin, config.postAttackDelayMax);
		ignoreFriends.setValue(config.ignoreFriends);
		requireSneaking.setValue(config.requireSneaking);
		overlayMessages.setValue(config.overlayMessages);
		// Order matters: the height slider writes min/max through the live tolerance, so the
		// center is applied first and the tolerance afterwards.
		targetHeight.setValue(currentTargetHeight());
		targetTolerance.setValue(currentTolerance());
		maxDistance.setValue(config.maxTargetDistance);
		maxHorizontalDistance.setValue(config.maxHorizontalDistance);
		// A profile or cloud config may also carry the master switch. Without this the combat
		// logic would run while the GUI still shows the module as OFF.
		setEnabled(config.enabled);
	}

	@Override
	protected void onEnabledChanged(boolean nowEnabled) {
		// Mirror the GUI state into the master switch the combat module reads.
		config.enabled = nowEnabled;
		ModConfig.requestSave(config);
	}

	// ------------------------------------------------------------------
	// Setting factories (each one writes into ModConfig and schedules a save)
	// ------------------------------------------------------------------

	private RangeSetting addRange(String name, double min, double max, double defaultMin, double defaultMax,
			RangeWriter writer) {
		RangeSetting setting = new RangeSetting(name, "ms", min, max, defaultMin, defaultMax, 1,
				(minValue, maxValue) -> {
					writer.write(minValue, maxValue);
					ModConfig.requestSave(config);
				});
		addSetting(setting);
		return setting;
	}

	private SliderSetting addSlider(String name, String unit, double min, double max, double defaultValue,
			double step, SettingWriter writer) {
		SliderSetting setting = new SliderSetting(name, unit, min, max, defaultValue, step,
				(value, unused) -> {
					writer.write(value);
					ModConfig.requestSave(config);
				});
		addSetting(setting);
		return setting;
	}

	private BooleanSetting addToggle(String name, boolean defaultValue, ToggleWriter writer) {
		BooleanSetting setting = new BooleanSetting(name, defaultValue, value -> {
			writer.write(value);
			ModConfig.requestSave(config);
		});
		addSetting(setting);
		return setting;
	}

	/** Writes a delay range into the combat config. */
	@FunctionalInterface
	private interface RangeWriter {
		void write(int min, int max);
	}

	/** Writes a single numeric value into the combat config. */
	@FunctionalInterface
	private interface SettingWriter {
		void write(double value);
	}

	/** Writes a switch state into the combat config. */
	@FunctionalInterface
	private interface ToggleWriter {
		void write(boolean value);
	}

	private double currentTargetHeight() {
		return (config.targetHeightMin + config.targetHeightMax) / 2.0;
	}

	private double currentTolerance() {
		return (config.targetHeightMax - config.targetHeightMin) / 2.0;
	}
}
