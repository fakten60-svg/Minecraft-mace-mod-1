package de.aerialmace.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tiny typed event bus for decoupling client systems ("event → manager/module → action").
 *
 * <p>Events are posted and delivered synchronously on the Minecraft client thread - the
 * mod has no background threads, so no further synchronization of handler state is needed.
 * The listener lists are copy-on-write so subscribing/unsubscribing while an event is
 * being delivered never throws.
 */
public final class EventBus {

	private static final Map<Class<?>, List<Consumer<Object>>> LISTENERS = new ConcurrentHashMap<>();

	private EventBus() {
	}

	/** Registers a listener for the given event type. Registering twice is harmless. */
	public static <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
		LISTENERS.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>())
				.add((Consumer<Object>) listener);
	}

	/** Removes a previously registered listener; unknown listeners are ignored. */
	public static <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
		List<Consumer<Object>> listeners = LISTENERS.get(eventType);
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	/** Delivers the event to every current listener of its type. Never throws. */
	public static void post(Object event) {
		List<Consumer<Object>> listeners = LISTENERS.get(event.getClass());
		if (listeners == null) {
			return;
		}
		for (Consumer<Object> listener : listeners) {
			try {
				listener.accept(event);
			} catch (RuntimeException e) {
				// One broken listener must not break the others or the caller.
				de.aerialmace.debug.ClientLogger.warn("Event listener failed for " + event.getClass().getSimpleName(), e);
			}
		}
	}
}
