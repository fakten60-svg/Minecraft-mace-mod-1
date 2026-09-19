package de.aerialmace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aerialmace.config.CloudConfigs;
import de.aerialmace.config.ConfigManager;
import de.aerialmace.config.ModConfig;
import de.aerialmace.friend.FriendManager;
import de.aerialmace.gui.Animation;
import de.aerialmace.hud.HudManager;
import de.aerialmace.input.KeybindManager;
import de.aerialmace.module.ModuleManager;
import de.aerialmace.module.modules.AutoGGModule;
import de.aerialmace.module.modules.ClientSettingsModule;
import de.aerialmace.module.modules.ESPModule;
import de.aerialmace.module.modules.FullbrightModule;
import de.aerialmace.module.modules.HUDModule;
import de.aerialmace.module.modules.MaceSwitchModule;
import de.aerialmace.module.modules.NoRotateModule;
import de.aerialmace.module.modules.SpeedModule;
import de.aerialmace.module.modules.SprintModule;
import de.aerialmace.module.modules.StepModule;
import de.aerialmace.sequence.SequenceStateMachine;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.MinecraftClient;

/**
 * Client entrypoint. Wires the module system onto the existing combat module and
 * registers the per-tick keybind handling. The combat logic
 * ({@link SequenceStateMachine}, {@link TargetSelector}) is NOT touched by the GUI —
 * it only reads {@link ModConfig}, which the module adapter writes.
 */
public class AerialMaceClient implements ClientModInitializer {

	public static final String MOD_ID = "aerialmace";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ModConfig activeConfig;

	private ModConfig config;
	private SequenceStateMachine stateMachine;
	private ClientSettingsModule clientSettings;

	@Override
	public void onInitializeClient() {
		config = ModConfig.load();
		activeConfig = config;
		FriendManager.load();
		stateMachine = new SequenceStateMachine(config);

		// ---- Module registry ----------------------------------------------------
		ModuleManager.register(new MaceSwitchModule(config)); // binds EXISTING combat logic
		ModuleManager.register(new ESPModule());
		ModuleManager.register(new FullbrightModule());
		ModuleManager.register(new HUDModule());
		ModuleManager.register(new SprintModule());
		ModuleManager.register(new SpeedModule());
		ModuleManager.register(new StepModule());
		ModuleManager.register(new AutoGGModule());
		ModuleManager.register(new NoRotateModule());
		clientSettings = new ClientSettingsModule(config);
		ModuleManager.register(clientSettings);
		// -------------------------------------------------------------------------

		ConfigManager.load(ConfigManager.newPanelPositionMap());
		HudManager.initialize();

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			stateMachine.reset();
			FriendManager.save();
			HudManager.save();
		});

		LOGGER.info("Aerial Mace Automation initialized (enabled: {})", config.enabled);
	}

	private void onEndClientTick(MinecraftClient client) {
		KeybindManager.tick(client, clientSettings.getState().guiKey);
		for (var module : ModuleManager.getModules()) {
			module.tick(client);
		}
		CloudConfigs.poll(config);
		ModConfig.flushPending();
		stateMachine.tick(client);
	}

	/** Shared handle for screens that need the live combat config (cloud config sharing). */
	public static ModConfig getConfig() {
		return activeConfig;
	}

	public static void saveClientData() {
		FriendManager.save();
	}

}
