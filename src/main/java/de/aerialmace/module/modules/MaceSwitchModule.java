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
 * <p>The module's enabled flag is mirrored to {@link ModConfig#enabled} so the previous
 * config file keeps working.
 */
public class MaceSwitchModule extends Module {

	private final ModConfig config;

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
		// Defaults come from the existing config, so previous values survive.
		// --------------------------------------------------------------
		addSetting(new RangeSetting("Initial Delay", "ms", 40, 400,
				config.initialDelayMin, config.initialDelayMax, 1,
				(min, max) -> {
					config.initialDelayMin = min;
					config.initialDelayMax = max;
					ModConfig.requestSave(config);
				}));

		addSetting(new RangeSetting("Equip Delay", "ms", 40, 400,
				config.equipToMaceDelayMin, config.equipToMaceDelayMax, 1,
				(min, max) -> {
					config.equipToMaceDelayMin = min;
					config.equipToMaceDelayMax = max;
					ModConfig.requestSave(config);
				}));

		addSetting(new RangeSetting("Attack Delay", "ms", 40, 400,
				config.maceToAttackDelayMin, config.maceToAttackDelayMax, 1,
				(min, max) -> {
					config.maceToAttackDelayMin = min;
					config.maceToAttackDelayMax = max;
					ModConfig.requestSave(config);
				}));

		// --------------------------------------------------------------
		// Targeting: height (center) and tolerance write the same two config
		// values the state machine compares against; both stay in sync.
		// --------------------------------------------------------------
		double center = (config.targetHeightMin + config.targetHeightMax) / 2.0;
		double tolerance = (config.targetHeightMax - config.targetHeightMin) / 2.0;

		addSetting(new SliderSetting("Target Height", "blocks", 0.5, 16.0, center, 0.05,
				(value, unused) -> {
					config.targetHeightMin = Math.max(0.0, value - tolerance);
					config.targetHeightMax = value + tolerance;
					ModConfig.requestSave(config);
				}));

		addSetting(new SliderSetting("Target Tolerance", "blocks", 0.05, 2.0, Math.max(tolerance, 0.05), 0.05,
				(value, unused) -> {
					double currentCenter = (config.targetHeightMin + config.targetHeightMax) / 2.0;
					config.targetHeightMin = Math.max(0.0, currentCenter - value);
					config.targetHeightMax = currentCenter + value;
					ModConfig.requestSave(config);
				}));

		addSetting(new SliderSetting("Max Distance", "blocks", 1.0, 32.0, config.maxTargetDistance, 0.5,
				(value, unused) -> {
					config.maxTargetDistance = value;
					if (config.maxHorizontalDistance > value) {
						config.maxHorizontalDistance = value;
					}
					ModConfig.requestSave(config);
				}));

		// --------------------------------------------------------------
		// Misc behavior switches bound to the existing config fields.
		// --------------------------------------------------------------
		addSetting(new BooleanSetting("Ignore Friends", config.ignoreFriends,
				value -> {
					config.ignoreFriends = value;
					ModConfig.requestSave(config);
				}));

		addSetting(new BooleanSetting("Require Sneaking", config.requireSneaking,
				value -> {
					config.requireSneaking = value;
					ModConfig.requestSave(config);
				}));

		addSetting(new BooleanSetting("Overlay Messages", config.overlayMessages,
				value -> {
					config.overlayMessages = value;
					ModConfig.requestSave(config);
				}));
	}

	@Override
	protected void onEnabledChanged(boolean nowEnabled) {
		// Mirror the GUI state into the master switch the combat module reads.
		config.enabled = nowEnabled;
		ModConfig.requestSave(config);
	}
}
