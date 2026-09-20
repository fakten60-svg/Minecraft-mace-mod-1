package de.aerialmace.input;

import java.util.HashMap;
import java.util.Map;

import de.aerialmace.gui.ClickGuiScreen;
import de.aerialmace.friend.FriendsScreen;
import de.aerialmace.config.CloudConfigsScreen;
import de.aerialmace.config.ConfigProfilesScreen;
import de.aerialmace.hud.HudEditorScreen;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleManager;
import de.aerialmace.module.setting.KeybindSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

/**
 * Handles keybinds outside the GUI screen. The GUI key (default RIGHT_SHIFT) toggles the
 * ClickGuiScreen; module keybinds default to {@link KeybindSetting#NONE} and only fire
 * once the user assigned a key in the GUI.
 *
 * <p>Keys are polled per client tick with edge detection (physical keyboard state), which
 * works regardless of focus and needs no Thread.sleep. While the ClickGUI is open the
 * screen itself handles keys, so polling is suspended.
 */
public final class KeybindManager {

	private static final Map<Integer, Boolean> PREVIOUS_STATES = new HashMap<>();

	private KeybindManager() {
	}

	/** Called once per client tick. */
	public static void tick(MinecraftClient client, int guiKeyCode) {
		// The ClickGuiScreen handles all keys itself while open (including recording). The
		// physical key states are still tracked: the GUI key closes the screen, and without
		// this the still-held key would immediately be seen as a fresh edge and reopen it.
		if (client.currentScreen instanceof ClickGuiScreen) {
			remember(client, guiKeyCode);
			for (Module module : ModuleManager.getModules()) {
				remember(client, module.getKeybind().getKey());
			}
			return;
		}
		if (client.player == null) {
			PREVIOUS_STATES.clear();
			return;
		}
		// Don't trigger anything while a vanilla screen (chat, inventory...) is open, but keep
		// the states in sync so no press is replayed once it closes.
		if (client.currentScreen != null) {
			remember(client, guiKeyCode);
			for (Module module : ModuleManager.getModules()) {
				remember(client, module.getKeybind().getKey());
			}
			return;
		}

		// GUI toggle key.
		if (guiKeyCode != KeybindSetting.NONE && pressedEdge(client, guiKeyCode)) {
			client.setScreen(new ClickGuiScreen());
			return;
		}

		// Module keybinds (user assigned only). They are checked before the reserved editor
		// keys, so a key the user explicitly bound to a module always reaches that module
		// instead of being silently swallowed by an editor shortcut.
		for (Module module : ModuleManager.getModules()) {
			KeybindSetting bind = module.getKeybind();
			if (bind.isNone()) {
				continue;
			}
			if (pressedEdge(client, bind.getKey())) {
				module.toggle();
				return;
			}
		}

		// Dedicated editors use reserved client keys and never leak into modules.
		if (pressedEdge(client, org.lwjgl.glfw.GLFW.GLFW_KEY_F6)) {
			client.setScreen(new ConfigProfilesScreen());
			return;
		}
		if (pressedEdge(client, org.lwjgl.glfw.GLFW.GLFW_KEY_F7)) {
			client.setScreen(new FriendsScreen());
			return;
		}
		if (pressedEdge(client, org.lwjgl.glfw.GLFW.GLFW_KEY_F8)) {
			client.setScreen(new HudEditorScreen());
			return;
		}
		if (pressedEdge(client, org.lwjgl.glfw.GLFW.GLFW_KEY_F9)) {
			client.setScreen(new CloudConfigsScreen(de.aerialmace.AerialMaceClient.getConfig()));
			return;
		}
		if (pressedEdge(client, org.lwjgl.glfw.GLFW.GLFW_KEY_F10)) {
			de.aerialmace.debug.DebugOverlay.toggle();
		}
	}

	private static boolean pressedEdge(MinecraftClient client, int keyCode) {
		boolean now = isPressed(client, keyCode);
		Boolean previous = PREVIOUS_STATES.put(keyCode, now);
		return now && !Boolean.TRUE.equals(previous);
	}

	/** Records the current state of a key without acting on it. */
	private static void remember(MinecraftClient client, int keyCode) {
		if (keyCode != KeybindSetting.NONE) {
			PREVIOUS_STATES.put(keyCode, isPressed(client, keyCode));
		}
	}

	private static boolean isPressed(MinecraftClient client, int keyCode) {
		if (KeybindSetting.isMouseCode(keyCode)) {
			return org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getHandle(),
					KeybindSetting.mouseCodeOf(keyCode)) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
		}
		return InputUtil.isKeyPressed(client.getWindow(), keyCode);
	}
}
