package de.aerialmace.module.modules;

import java.util.List;

import de.aerialmace.module.Module;
import de.aerialmace.module.ModuleCategory;
import de.aerialmace.module.setting.BooleanSetting;
import de.aerialmace.module.setting.ModeSetting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** Legitimate sprint helper; changes only the local sprint input state, not packets. */
public final class SprintModule extends Module {
	private String mode = "Legit";
	private boolean omnidirectional;
	private boolean keepSprint;
	private boolean requireForward = true;

	public SprintModule() {
		super("Sprint", "Automatically sprints while movement permits it.", ModuleCategory.MOVEMENT);
	}

	@Override
	protected void registerSettings() {
		addSetting(new ModeSetting("Mode", List.of("Legit", "Rage"), "Legit", value -> mode = value));
		addSetting(new BooleanSetting("Omnidirectional", false, value -> omnidirectional = value));
		addSetting(new BooleanSetting("Keep Sprint", false, value -> keepSprint = value));
		addSetting(new BooleanSetting("Require Forward", true, value -> requireForward = value));
	}

	@Override
	protected void onTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || player.isDead() || player.isSpectator()) return;

		boolean movingForward = player.forwardSpeed > 0.0f;
		boolean movingSideways = Math.abs(player.sidewaysSpeed) > 0.0f;
		boolean hasMovement = movingForward || movingSideways;
		boolean allowed = hasMovement && (!requireForward || movingForward || omnidirectional);
		if (mode.equals("Legit")) {
			allowed = allowed && !player.isSneaking() && player.getHungerManager().getFoodLevel() > 6;
		}
		if (allowed && (keepSprint || !player.isSprinting())) {
			player.setSprinting(true);
		} else if (!allowed && !keepSprint) {
			player.setSprinting(false);
		}
	}
}
