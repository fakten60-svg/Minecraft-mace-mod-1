package de.aerialmace.input;

import java.util.HashMap;
import java.util.Map;

import de.aerialmace.gui.ClickGuiScreen;
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
		// The ClickGuiScreen handles all keys itself while open (including recording).
		if (client.currentScreen instanceof ClickGuiScreen || client.player == null) {
			PREVIOUS_STATES.clear();
			return;
		}
		// Don't trigger anything while a vanilla screen (chat, inventory...) is open.
		if (client.currentScreen != null) {
			PREVIOUS_STATES.clear();
			return;
		}

		// GUI toggle key.
		if (guiKeyCode != KeybindSetting.NONE && pressedEdge(client, guiKeyCode)) {
			client.setScreen(new ClickGuiScreen());
			return;
		}

		// Module keybinds (user-assigned only).
		for (Module module : ModuleManager.getModules()) {
			KeybindSetting bind = module.getKeybind();
			if (bind.isNone()) {
				continue;
			}
			if (pressedEdge(client, bind.getKey())) {
				module.toggle();
			}
		}
	}

	private static boolean pressedEdge(MinecraftClient client, int keyCode) {
		boolean now;
		if (KeybindSetting.isMouseCode(keyCode)) {
			now = org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getHandle(),
					KeybindSetting.mouseCodeOf(keyCode)) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
		} else {
			now = InputUtil.isKeyPressed(client.getWindow(), keyCode);
		}
		Boolean previous = PREVIOUS_STATES.get(keyCode);
		PREVIOUS_STATES.put(keyCode, now);
		return now && !Boolean.TRUE.equals(previous);
	}
}
