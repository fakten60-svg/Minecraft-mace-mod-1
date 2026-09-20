package de.aerialmace.gui;

/**
 * Tiny FPS-independent animation helper. Uses wall-clock time, so it is frame rate
 * independent; no Thread.sleep anywhere.
 */
public final class Animation {

	/** Global speed multiplier from the client settings (Animation Speed). */
	private static volatile float globalSpeed = 1.0f;

	public static void setGlobalSpeed(float speed) {
		globalSpeed = Math.max(0.1f, speed);
	}

	private final float speed; // 1.0 = default speed
	private float progress;    // 0..1
	private boolean open;

	public Animation(float speed) {
		this.speed = Math.max(0.05f, speed);
	}

	/** Call once per frame with the delta time in seconds. */
	public void update(float deltaSeconds) {
		float step = deltaSeconds * speed * globalSpeed * 14.0f; // exponential smoothing factor
		float target = open ? 1.0f : 0.0f;
		float diff = target - progress;
		if (Math.abs(diff) < 0.002f) {
			progress = target;
		} else {
			progress += diff * Math.min(1.0f, step);
		}
	}

	public void open() {
		open = true;
	}

	public void close() {
		open = false;
	}

	public boolean isOpen() {
		return open;
	}

	/** Jumps to the target immediately (no animation). */
	public void setInstant(boolean open) {
		this.open = open;
		this.progress = open ? 1.0f : 0.0f;
	}

	/** Current 0..1 progress (smoothed). */
	public float value() {
		return progress;
	}

	/** Smooth-step eased value for nicer motion. */
	public float eased() {
		float t = progress;
		return t * t * (3 - 2 * t);
	}

}
