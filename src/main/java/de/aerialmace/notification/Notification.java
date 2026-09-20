package de.aerialmace.notification;

/**
 * A single on-screen notification. Age and slide progress are advanced by
 * {@link NotificationManager} with the real frame delta, so animations are FPS-independent
 * and nothing blocks or sleeps.
 */
public final class Notification {

	/** Higher priority notifications stay longer on screen. */
	public enum Priority {
		NORMAL, HIGH
	}

	private final NotificationType type;
	private final String title;
	private final String message;
	private final Priority priority;
	private final float durationSeconds;

	private float ageSeconds;
	private float slideProgress; // 0 = fully hidden right, 1 = fully shown

	Notification(NotificationType type, String title, String message, Priority priority) {
		this.type = type;
		this.title = title;
		this.message = message;
		this.priority = priority;
		this.durationSeconds = priority == Priority.HIGH
				? type.defaultDurationSeconds() * 1.6f
				: type.defaultDurationSeconds();
	}

	public NotificationType type() {
		return type;
	}

	public String title() {
		return title;
	}

	public String message() {
		return message;
	}

	public Priority priority() {
		return priority;
	}

	public float slideProgress() {
		return slideProgress;
	}

	/** Advances the animation by the real frame delta; returns false once expired. */
	boolean update(float deltaSeconds) {
		ageSeconds += deltaSeconds;
		slideProgress += (1.0f - slideProgress) * Math.min(1.0f, deltaSeconds * 12.0f);
		return ageSeconds < durationSeconds;
	}

	/** 1..0 fade/slide-out factor applied during the last quarter second. */
	float fadeOutFactor() {
		float remaining = durationSeconds - ageSeconds;
		if (remaining >= 0.25f) {
			return 1.0f;
		}
		return Math.max(0.0f, remaining / 0.25f);
	}
}
