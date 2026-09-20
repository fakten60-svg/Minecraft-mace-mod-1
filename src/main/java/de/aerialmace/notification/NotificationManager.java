package de.aerialmace.notification;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import de.aerialmace.AerialMaceClient;
import de.aerialmace.gui.ThemeManager;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Central notification system, independent of single modules. Notifications are queued,
 * animated with the real frame delta (FPS-independent, no Thread.sleep) and rendered as a
 * HUD overlay at the bottom right. The queue is capped so a misbehaving caller cannot
 * flood the screen.
 *
 * <p>Events are reported to this manager explicitly (module toggle, config errors, profile
 * actions) - the manager never scans or reaches into other systems itself.
 */
public final class NotificationManager {

	private static final int MAX_QUEUE = 8;
	private static final int GAP = 4;
	private static final int PADDING = 7;
	private static final float MAX_FRAME_SECONDS = 0.1f; // clamp alt-tab pauses
	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private static final ArrayDeque<Notification> QUEUE = new ArrayDeque<>();
	private static long lastFrameNanos = System.nanoTime();
	private static boolean registered;

	private NotificationManager() {
	}

	/** Shows a notification with the type's default duration. */
	public static void notify(NotificationType type, String title, String message) {
		notify(type, title, message, null);
	}

	/** Shows a notification; {@code priority} may be null for NORMAL. */
	public static void notify(NotificationType type, String title, String message,
			@Nullable Notification.Priority priority) {
		if (title == null || title.isBlank()) {
			return;
		}
		if (QUEUE.size() >= MAX_QUEUE) {
			QUEUE.pollFirst(); // drop the oldest instead of growing unbounded
		}
		QUEUE.addLast(new Notification(type, title, message == null ? "" : message, priority));
		register();
	}

	public static boolean hasPending() {
		return !QUEUE.isEmpty();
	}

	/**
	 * Subscribes the built-in reactions to client events. Called once from the client
	 * bootstrap, after the module system exists.
	 */
	public static void init() {
		de.aerialmace.event.EventBus.subscribe(de.aerialmace.event.ModuleToggleEvent.class, event ->
				notify(event.enabled() ? NotificationType.SUCCESS : NotificationType.INFO,
						event.enabled() ? "Module enabled" : "Module disabled",
						event.module().getName()));
	}

	/** Registers the HUD render hook once; rendering happens on the client thread only. */
	private static void register() {
		if (registered) {
			return;
		}
		registered = true;
		HudElementRegistry.addLast(Identifier.of(AerialMaceClient.MOD_ID, "notifications"),
				(context, tickCounter) -> render(context, MinecraftClient.getInstance()));
	}

	private static void render(DrawContext context, MinecraftClient client) {
		if (QUEUE.isEmpty()) {
			return;
		}
		// FPS-independent timing, clamped so alt-tab pauses do not fast-forward everything.
		long now = System.nanoTime();
		float deltaSeconds = Math.min(MAX_FRAME_SECONDS, (now - lastFrameNanos) / (float) NANOS_PER_SECOND);
		lastFrameNanos = now;

		List<Notification> alive = new ArrayList<>(QUEUE.size());
		while (!QUEUE.isEmpty()) {
			Notification notification = QUEUE.pollFirst();
			if (notification.update(deltaSeconds)) {
				alive.add(notification);
			}
		}
		QUEUE.addAll(alive);
		if (alive.isEmpty()) {
			return;
		}

		var textRenderer = client.textRenderer;
		ThemeManager theme = ThemeManager.get();
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		int y = screenHeight - 10;

		for (int index = alive.size() - 1; index >= 0; index--) {
			Notification notification = alive.get(index);
			int width = Math.max(
					textRenderer.getWidth(notification.title()),
					textRenderer.getWidth(notification.message())) + PADDING * 2 + 4;
			int height = notification.message().isEmpty() ? 15 : 24;
			int top = y - height;

			// Slide in from the right; slide back out during the fade-out phase.
			float slide = notification.slideProgress() * notification.fadeOutFactor();
			int x = screenWidth - Math.round(width * slide);

			if (x < screenWidth) {
				context.fill(x, top, x + width, y, theme.panel().getColor());
				// Accent bar on the left edge marks the severity.
				context.fill(x, top, x + 2, y, notification.type().accentColor());
				int textX = x + PADDING + 2;
				int textAlpha = Math.round(255 * notification.fadeOutFactor());
				context.drawText(textRenderer, notification.title(), textX, top + 3,
						ThemeManager.withAlpha(theme.text().getColor(), textAlpha), false);
				if (!notification.message().isEmpty()) {
					context.drawText(textRenderer, notification.message(), textX, top + 13,
							ThemeManager.withAlpha(theme.secondaryText().getColor(), textAlpha), false);
				}
			}
			y = top - GAP;
		}
	}
}
