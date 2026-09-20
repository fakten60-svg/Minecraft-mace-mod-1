package de.aerialmace.target;

import java.util.Comparator;
import java.util.Optional;

import de.aerialmace.config.ModConfig;
import de.aerialmace.friend.FriendManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Finds a suitable target player for the aerial mace sequence.
 *
 * <p>A player qualifies when
 * <ul>
 *   <li>they are alive and not the local player,</li>
 *   <li>they are on the local player's team (or team rules do not apply),</li>
 *   <li>the vertical delta {@code playerY - targetY} lies within the configured
 *       {@code [targetHeightMin, targetHeightMax]} window (roughly 3 blocks with tolerance),</li>
 *   <li>the total and horizontal distances are within the configured limits.</li>
 * </ul>
 *
 * <p>If several players qualify, the closest one is chosen. The team check uses the client
 * player list so remote players are classified without touching server-side state.
 */
public final class TargetSelector {

	private TargetSelector() {
	}

	/** Result of a target check: the player plus the measured distances. */
	public record TargetInfo(PlayerEntity player, double verticalDelta, double horizontalDistance, double totalDistance) {
	}

	/**
	 * Searches the client world for the best qualifying target player, if any.
	 */
	public static Optional<TargetInfo> findTarget(MinecraftClient client, ModConfig config) {
		ClientPlayerEntity self = client.player;
		if (self == null || client.world == null || self.isSpectator()) {
			return Optional.empty();
		}

		return client.world.getPlayers().stream()
				.filter(player -> player != self)
				.filter(player -> !player.isRemoved() && player.isAlive() && player.getHealth() > 0.0F)
				.filter(player -> !self.isSpectator() && !player.isSpectator())
				.filter(player -> !player.isInvisible())
				.filter(player -> !config.ignoreFriends || !FriendManager.isFriend(player))
				.filter(player -> isFriendlyContext(self, player))
				.map(player -> toInfo(self, player))
				.filter(info -> info.verticalDelta() >= config.targetHeightMin
						&& info.verticalDelta() <= config.targetHeightMax)
				.filter(info -> info.totalDistance() <= config.maxTargetDistance)
				.filter(info -> info.horizontalDistance() <= config.maxHorizontalDistance)
				.min(Comparator.comparingDouble(TargetInfo::totalDistance));
	}

	/**
	 * Re-validates a previously found target against the current state of the world.
	 * Used right before the attack so a vanished or moved player is never hit.
	 */
	public static boolean isStillValid(PlayerEntity target, ClientPlayerEntity self, ModConfig config) {
		if (target == null || self == null || self.isRemoved() || self.isSpectator()) {
			return false;
		}
		if (target.isRemoved() || !target.isAlive() || target.getHealth() <= 0.0F) {
			return false;
		}
		if (config.ignoreFriends && FriendManager.isFriend(target)) {
			return false;
		}
		if (target.getEntityWorld() != self.getEntityWorld()) {
			return false;
		}
		TargetInfo info = toInfo(self, target);
		return info.verticalDelta() >= config.targetHeightMin
				&& info.verticalDelta() <= config.targetHeightMax
				&& info.totalDistance() <= config.maxTargetDistance
				&& info.horizontalDistance() <= config.maxHorizontalDistance;
	}

	private static TargetInfo toInfo(ClientPlayerEntity self, PlayerEntity player) {
		double dx = player.getX() - self.getX();
		double dz = player.getZ() - self.getZ();
		double dy = self.getY() - player.getY();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double total = Math.sqrt(dx * dx + dy * dy + dz * dz);
		return new TargetInfo(player, dy, horizontal, total);
	}

	/**
	 * Rough friendly-fire context: never target players that share our team (or are on our
	 * scoreboard team at all) unless no team information is available.
	 */
	private static boolean isFriendlyContext(ClientPlayerEntity self, PlayerEntity candidate) {
		var ownTeam = self.getScoreboardTeam();
		var otherTeam = candidate.getScoreboardTeam();
		if (ownTeam == null || otherTeam == null) {
			return true;
		}
		return !ownTeam.equals(otherTeam);
	}

}
