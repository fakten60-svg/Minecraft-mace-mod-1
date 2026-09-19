package de.aerialmace;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.aerialmace.config.ModConfig;
import de.aerialmace.sequence.SequenceStateMachine;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

/**
 * Client entrypoint: loads the config, registers the toggle key binding and drives the
 * sequence state machine once per client tick. The machine is also reset whenever the
 * client disconnects from a world/server.
 */
public class AerialMaceClient implements ClientModInitializer {

	public static final String MOD_ID = "aerialmace";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private ModConfig config;
	private SequenceStateMachine stateMachine;
	private KeyBinding toggleKey;

	@Override
	public void onInitializeClient() {
		config = ModConfig.load();
		stateMachine = new SequenceStateMachine(config);

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.aerialmace.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stateMachine.reset());

		LOGGER.info("Aerial Mace Automation initialized (enabled: {})", config.enabled);
	}

	private void onEndClientTick(MinecraftClient client) {
		while (toggleKey.wasPressed()) {
			config.enabled = !config.enabled;
			ModConfig.save(config);
			if (!config.enabled) {
				stateMachine.reset();
			}
			showToggleMessage(client, config.enabled);
		}
		stateMachine.tick(client);
	}

	private void showToggleMessage(MinecraftClient client, boolean enabled) {
		if (client.player != null) {
			client.player.sendMessage(Text.literal(enabled
					? "§b[AerialMace] §fAutomatisierung §aaktiviert"
					: "§b[AerialMace] §fAutomatisierung §cdeaktiviert"), true);
		}
	}
}
