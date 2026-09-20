package de.aerialmace.sequence;

import java.util.function.Consumer;

import de.aerialmace.config.ModConfig;
import de.aerialmace.target.TargetSelector;
import de.aerialmace.util.Delays;
import de.aerialmace.util.InventoryUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import org.jetbrains.annotations.Nullable;

/**
 * Non-blocking state machine that runs the aerial mace sequence on the client tick thread.
 *
 * <p>States: {@code IDLE -> TARGET_FOUND -> INITIAL_DELAY -> EQUIP_CHESTPLATE -> EQUIP_DELAY
 * -> SWITCH_TO_MACE -> MACE_DELAY -> ATTACK -> IDLE}.
 *
 * <p>All delays are randomized inclusive millisecond ranges taken from {@link ModConfig}; the
 * machine only records a deadline and continues on a later client tick once it passes — there
 * are no {@code Thread.sleep} calls anywhere. Actions run in the same tick their delay expires
 * so the configured timing stays exact.
 *
 * <p>Every transition re-checks its preconditions, so the sequence aborts cleanly as soon as
 * any requirement (world, alive, target, height/distance window, mace availability) stops
 * holding. Aborting restores the original hotbar slot.
 */
public final class SequenceStateMachine {

	/** The phases of the sequence, in execution order. */
	public enum State {
		IDLE,
		TARGET_FOUND,
		TARGET_LOCK_DELAY,
		INITIAL_DELAY,
		EQUIP_CHESTPLATE,
		EQUIP_DELAY,
		SWITCH_TO_MACE,
		MACE_DELAY,
		ATTACK,
		POST_ATTACK_DELAY
	}

	/** Hard cap so a wedged machine can never keep a sequence alive forever. */
	private static final long MAX_SEQUENCE_DURATION_MS = 5_000L;

	private final ModConfig config;

	private State state = State.IDLE;
	private long stateEnteredAtMs = 0L;
	private long currentDelayMs = 0L;
	private long sequenceStartedAtMs = 0L;
	private int originalHotbarSlot = 0;
	private boolean hasOriginalSlot = false;

	@Nullable
	private PlayerEntity target;

	@Nullable
	private Runnable statusListener;

	public SequenceStateMachine(ModConfig config) {
		this.config = config;
	}

	public State getState() {
		return state;
	}

	public boolean isRunning() {
		return state != State.IDLE;
	}

	public void setStatusListener(@Nullable Runnable statusListener) {
		this.statusListener = statusListener;
	}

	/** Resets the machine back to {@link State#IDLE}. Called when leaving a world, for example. */
	public void reset() {
		state = State.IDLE;
		target = null;
		stateEnteredAtMs = 0L;
		currentDelayMs = 0L;
		sequenceStartedAtMs = 0L;
		originalHotbarSlot = 0;
		hasOriginalSlot = false;
	}

	/**
	 * Main entry point, called once per client tick. Does nothing unless enabled.
	 */
	public void tick(MinecraftClient client) {
		if (!config.enabled) {
			if (isRunning()) {
				abort(client, "deaktiviert");
			}
			return;
		}

		switch (state) {
			case IDLE -> tryStart(client);
			case TARGET_FOUND -> beginTargetLockDelay(client);
			case TARGET_LOCK_DELAY -> waitAndThen(client, State.INITIAL_DELAY, this::beginInitialDelay);
			case INITIAL_DELAY -> waitAndThen(client, State.EQUIP_CHESTPLATE, this::performEquipChestplate);
			case EQUIP_CHESTPLATE -> performEquipChestplate(client);
			case EQUIP_DELAY -> waitAndThen(client, State.SWITCH_TO_MACE, this::performSwitchToMace);
			case SWITCH_TO_MACE -> performSwitchToMace(client);
			case MACE_DELAY -> waitAndThen(client, State.ATTACK, this::performAttack);
			case ATTACK -> performAttack(client);
			case POST_ATTACK_DELAY -> finishPostAttackDelay(client);
		}
	}

	// ------------------------------------------------------------------
	// Start conditions
	// ------------------------------------------------------------------

	private void tryStart(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.interactionManager == null) {
			return;
		}
		if (player.isDead() || !player.isAlive() || player.isSpectator()) {
			return;
		}
		if (config.requireSneaking && !player.isSneaking()) {
			return;
		}

		TargetSelector.TargetInfo info = TargetSelector.findTarget(client, config).orElse(null);
		if (info == null) {
			return;
		}
		originalHotbarSlot = player.getInventory().getSelectedSlot();
		hasOriginalSlot = true;
		target = info.player();
		transition(State.TARGET_FOUND, client);
	}

	// ------------------------------------------------------------------
	// Phase 1: chestplate
	// ------------------------------------------------------------------

	private void beginTargetLockDelay(MinecraftClient client) {
		if (!preconditionsHold(client)) {
			return;
		}
		currentDelayMs = Delays.randomDelay(config.targetLockDelayMin, config.targetLockDelayMax);
		transition(State.TARGET_LOCK_DELAY, client);
	}

	private void beginInitialDelay(MinecraftClient client) {
		if (!preconditionsHold(client)) {
			return;
		}
		currentDelayMs = Delays.randomDelay(config.initialDelayMin, config.initialDelayMax);
		transition(State.INITIAL_DELAY, client);
	}

	private void performEquipChestplate(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.interactionManager == null) {
			abort(client, "keine Interaktion möglich");
			return;
		}
		if (!InventoryUtils.equipHeldItemAsChestplate(client, player)) {
			abort(client, "keine Brustplatte ausrüstbar");
			return;
		}
		currentDelayMs = Delays.randomDelay(config.equipToMaceDelayMin, config.equipToMaceDelayMax);
		transition(State.EQUIP_DELAY, client);
	}

	// ------------------------------------------------------------------
	// Phase 2: mace
	// ------------------------------------------------------------------

	private void performSwitchToMace(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.interactionManager == null) {
			abort(client, "keine Interaktion möglich");
			return;
		}
		int maceSlot = InventoryUtils.findMaceSlot(player);
		if (maceSlot < 0) {
			abort(client, "keine Mace in der Hotbar");
			return;
		}
		InventoryUtils.switchHotbarSlot(client, player, maceSlot);
		currentDelayMs = Delays.randomDelay(config.maceToAttackDelayMin, config.maceToAttackDelayMax);
		transition(State.MACE_DELAY, client);
	}

	// ------------------------------------------------------------------
	// Phase 3: attack
	// ------------------------------------------------------------------

	private void performAttack(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		ClientPlayerInteractionManager interactionManager = client.interactionManager;
		if (player == null || interactionManager == null || client.world == null) {
			abort(client, "keine Interaktion möglich");
			return;
		}

		// Re-validate the target one final time right before hitting.
		PlayerEntity currentTarget = target;
		if (currentTarget == null || currentTarget.isRemoved() || !currentTarget.isAlive()
				|| currentTarget.getHealth() <= 0.0F
				|| currentTarget.getEntityWorld() != player.getEntityWorld()
				|| !TargetSelector.isStillValid(currentTarget, player, config)) {
			abort(client, "Ziel verschwunden");
			return;
		}

		// The mace must still be in hand after the hotbar switch.
		ItemStack held = player.getMainHandStack();
		if (!(held.getItem() instanceof MaceItem)) {
			abort(client, "Mace nicht mehr in der Hand");
			return;
		}

		// Send exactly the packets a real left click on the entity produces.
		interactionManager.attackEntity(player, currentTarget);
		player.swingHand(Hand.MAIN_HAND);

		currentDelayMs = Delays.randomDelay(config.postAttackDelayMin, config.postAttackDelayMax);
		transition(State.POST_ATTACK_DELAY, client);
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private void finishPostAttackDelay(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			reset();
			return;
		}
		if (nowMs() - stateEnteredAtMs >= currentDelayMs) {
			transition(State.IDLE, client);
		}
	}

	private void waitAndThen(MinecraftClient client, State actionState, Consumer<MinecraftClient> action) {
		if (!preconditionsHold(client)) {
			return;
		}
		if (nowMs() - stateEnteredAtMs < currentDelayMs) {
			return;
		}
		// Pass through the action state and run the action in this very tick so the
		// configured delay is not extended by an extra tick.
		transition(actionState, client);
		action.accept(client);
	}

	private boolean preconditionsHold(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.interactionManager == null) {
			abort(client, "Welt nicht verfügbar");
			return false;
		}
		if (player.isDead() || !player.isAlive() || player.isSpectator()) {
			abort(client, "Spieler tot/unfähig");
			return false;
		}
		if (target != null && (target.isRemoved() || !target.isAlive() || target.getHealth() <= 0.0F)) {
			abort(client, "Ziel verschwunden");
			return false;
		}
		if (target != null && !TargetSelector.isStillValid(target, player, config)) {
			abort(client, "Ziel außerhalb des Fensters");
			return false;
		}
		if (nowMs() - sequenceStartedAtMs > MAX_SEQUENCE_DURATION_MS) {
			abort(client, "Timeout");
			return false;
		}
		return true;
	}

	private long nowMs() {
		return System.currentTimeMillis();
	}

	private void transition(State newState, MinecraftClient client) {
		state = newState;
		stateEnteredAtMs = nowMs();
		if (newState == State.TARGET_FOUND) {
			sequenceStartedAtMs = stateEnteredAtMs;
			showStatus(client, "Ziel gefunden – Sequenz läuft");
		}
		if (newState == State.IDLE) {
			target = null;
			hasOriginalSlot = false;
		}
		notifyStatusListener();
	}

	private void abort(MinecraftClient client, String reason) {
		ClientPlayerEntity player = client.player;
		if (player != null && hasOriginalSlot) {
			InventoryUtils.switchHotbarSlot(client, player, originalHotbarSlot);
		}
		if (client != null && client.player != null && config.overlayMessages) {
			client.inGameHud.setOverlayMessage(
					Text.literal("§c[" + ModConfig.CLIENT_NAME + "] §7Abgebrochen: §f" + reason), false);
		}
		reset();
	}

	private void showStatus(MinecraftClient client, String message) {
		if (client != null && client.player != null && config.overlayMessages) {
			client.inGameHud.setOverlayMessage(Text.literal("§b[" + ModConfig.CLIENT_NAME + "] §f" + message), false);
		}
	}

	private void notifyStatusListener() {
		Runnable listener = statusListener;
		if (listener != null) {
			listener.run();
		}
	}
}
