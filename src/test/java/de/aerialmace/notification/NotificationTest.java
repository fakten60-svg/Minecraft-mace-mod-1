package de.aerialmace.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the notification lifetime logic: FPS-independent ageing, fade-out windows and
 * the priority-based duration bonus. Pure logic only - no rendering involved.
 */
class NotificationTest {

	@Test
	void newNotificationIsFullyVisibleForItsLifetime() {
		Notification notification = new Notification(NotificationType.INFO, "T", "M", null);

		assertTrue(notification.update(0.5f));
		assertEquals(1.0f, notification.fadeOutFactor(), 1e-6);
	}

	@Test
	void notificationExpiresAfterItsDuration() {
		Notification notification = new Notification(NotificationType.INFO, "T", "M", null);

		// INFO lives 3 seconds; just under and just over.
		assertTrue(notification.update(2.9f));
		assertFalse(notification.update(0.2f));
	}

	@Test
	void highPriorityLivesLongerThanNormal() {
		Notification normal = new Notification(NotificationType.WARNING, "T", "M", null);
		Notification high = new Notification(NotificationType.WARNING, "T", "M",
				Notification.Priority.HIGH);

		// WARNING lives 5s (x1.6 for HIGH = 8s): after 6s the normal one is expired,
		// the HIGH priority one must still be alive.
		assertFalse(normal.update(6.0f), "normal priority expired after 6s");
		assertTrue(high.update(6.0f), "HIGH priority must still be alive after 6s");
	}

	@Test
	void fadeOutOnlyAppliesNearTheEnd() {
		Notification notification = new Notification(NotificationType.ERROR, "T", "M", null);

		notification.update(4.0f); // ERROR lives 6s -> 2s remaining
		assertEquals(1.0f, notification.fadeOutFactor(), 1e-6);

		notification.update(1.9f); // 0.1s remaining -> inside the fade window
		assertTrue(notification.fadeOutFactor() < 1.0f);
		assertTrue(notification.fadeOutFactor() >= 0.0f);
	}

	@Test
	void slideProgressApproachesOneWithoutOvershooting() {
		Notification notification = new Notification(NotificationType.INFO, "T", "M", null);

		for (int i = 0; i < 60; i++) {
			notification.update(0.05f);
			float slide = notification.slideProgress();
			assertTrue(slide > 0.0f && slide <= 1.0f, "slide out of bounds: " + slide);
		}
		assertTrue(notification.slideProgress() > 0.95f, "slide should be nearly complete");
	}

	@Test
	void blankTitlesAreRejectedByTheManager() {
		NotificationManager.notify(NotificationType.INFO, "   ", "no title");
		assertFalse(NotificationManager.hasPending(), "blank titles must not enqueue");
	}
}
