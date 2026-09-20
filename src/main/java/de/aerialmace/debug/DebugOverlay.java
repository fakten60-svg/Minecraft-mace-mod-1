package de.aerialmace.debug;

import java.util.ArrayList;
import java.util.List;

import de.aerialmace.AerialMaceClient;
import de.aerialmace.config.ConfigManager;
import de.aerialmace.config.ModConfig;
import de.aerialmace.gui.ThemeManager;
import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleManager;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Compact on-screen diagnostics (F10 toggle, off by default). Shows real values only:
 * FPS, current screen, GUI scale, config dirty state, active modules, the combat state
 * machine phase and the actual mod/loader versions from the Fabric loader metadata.
 *
 * <p>Toggling also enables {@link ClientLogger} debug output, so one key controls both.
 * The overlay is intentionally small and never blocks input.
 */
public final class DebugOverlay {

	private static boolean visible;
	private static boolean registered;

	private DebugOverlay() {
	}

	public static boolean isVisible() {
		return visible;
	}

	/** Shows/hides the overlay; enabling also turns on debug logging and the HUD hook. */
	public static void setVisible(boolean value) {
		visible = value;
		ClientLogger.setDebugEnabled(value);
		if (visible && !registered) {
			registered = true;
			HudElementRegistry.addLast(Identifier.of(AerialMaceClient.MOD_ID, "debug"),
					(context, tickCounter) -> render(context, MinecraftClient.getInstance()));
		}
	}

	public static void toggle() {
		setVisible(!visible);
		de.aerialmace.notification.NotificationManager.notify(
				de.aerialmace.notification.NotificationType.INFO, "Debug overlay",
				visible ? "enabled" : "disabled");
	}

	/** Renders the overlay when enabled; called from the client tick/render bridge. */
	public static void render(DrawContext context, MinecraftClient client) {
		if (!visible || client.player == null) {
			return;
		}
		var textRenderer = client.textRenderer;
		ThemeManager theme = ThemeManager.get();
		List<String> lines = collectLines(client);

		int width = 0;
		for (String line : lines) {
			width = Math.max(width, textRenderer.getWidth(line));
		}
		int height = lines.size() * 11 + 8;
		context.fill(4, 4, 4 + width + 10, 4 + height, theme.panel().getColor());
		context.fill(4, 4, 5, 4 + height, theme.accent().getColor());

		int y = 8;
		for (String line : lines) {
			context.drawText(textRenderer, line, 10, y, theme.text().getColor(), false);
			y += 11;
		}
	}

	private static List<String> collectLines(MinecraftClient client) {
		List<String> lines = new ArrayList<>();
		lines.add(ModConfig.CLIENT_NAME + " " + version() + " (debug)");
		lines.add("MC " + containerVersion("minecraft") + " · Fabric " + containerVersion("fabricloader"));
		lines.add("FPS " + client.getCurrentFps()
				+ " · Screen " + (client.currentScreen == null ? "none" : client.currentScreen.getClass().getSimpleName()));
		lines.add("GUI scale " + guiScale());
		lines.add("Config dirty: " + (ConfigManager.isDirty() ? "yes" : "no"));
		lines.add("Sequence: " + combatState());
		lines.add("Modules: " + activeModules());
		return lines;
	}

	private static String version() {
		return containerVersion(AerialMaceClient.MOD_ID);
	}

	/** Real version from the Fabric loader metadata - never a hardcoded value. */
	private static String containerVersion(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("?");
	}

	private static String guiScale() {
		var settings = de.aerialmace.module.modules.ClientSettingsModule.get();
		return settings != null ? String.valueOf(settings.getState().guiScale) : "1.0";
	}

	private static String combatState() {
		return AerialMaceClient.stateMachineState().toString();
	}

	private static String activeModules() {
		List<String> active = new ArrayList<>();
		for (Module module : ModuleManager.getModules()) {
			if (module.isEnabled()) {
				active.add(module.getName());
			}
		}
		return active.isEmpty() ? "none" : String.join(", ", active);
	}
}
