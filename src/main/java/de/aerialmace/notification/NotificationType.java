package de.aerialmace.notification;

/**
 * Severity of a notification. Each type carries its semantic accent color (status colors
 * are intentionally fixed, not theme colors - red always means error) and the default
 * time it stays on screen.
 */
public enum NotificationType {

	INFO(0xFF60A5FA, 3.0f),
	SUCCESS(0xFF4ADE80, 3.0f),
	WARNING(0xFFFBBF24, 5.0f),
	ERROR(0xFFF87171, 6.0f);

	private final int accentColor;
	private final float defaultDurationSeconds;

	NotificationType(int accentColor, float defaultDurationSeconds) {
		this.accentColor = accentColor;
		this.defaultDurationSeconds = defaultDurationSeconds;
	}

	public int accentColor() {
		return accentColor;
	}

	public float defaultDurationSeconds() {
		return defaultDurationSeconds;
	}
}
