package de.aerialmace.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for the tiny event bus: typed delivery, unsubscribe and the guarantee that one
 * broken listener cannot break the others.
 */
class EventBusTest {

	private record SampleEvent(String name) {
	}

	@Test
	void deliversToSubscribersOfTheSameType() {
		AtomicInteger count = new AtomicInteger();
		EventBus.subscribe(SampleEvent.class, event -> count.incrementAndGet());

		EventBus.post(new SampleEvent("a"));

		assertEquals(1, count.get());
	}

	@Test
	void unsubscribedListenersAreNotCalled() {
		AtomicInteger count = new AtomicInteger();
		EventBus.subscribe(SampleEvent.class, event -> count.incrementAndGet());
		// Re-fetching the identical lambda is impossible, so subscribe a named listener.
		java.util.function.Consumer<SampleEvent> listener = event -> count.incrementAndGet();
		EventBus.subscribe(SampleEvent.class, listener);
		EventBus.unsubscribe(SampleEvent.class, listener);

		EventBus.post(new SampleEvent("a"));

		assertEquals(1, count.get(), "only the first subscriber should have been called");
	}

	@Test
	void oneBrokenListenerDoesNotBreakTheOthers() {
		AtomicInteger count = new AtomicInteger();
		EventBus.subscribe(SampleEvent.class, event -> {
			throw new IllegalStateException("boom");
		});
		EventBus.subscribe(SampleEvent.class, event -> count.incrementAndGet());

		EventBus.post(new SampleEvent("a"));

		assertEquals(1, count.get(), "listeners registered after a throwing one still run");
	}

	@Test
	void eventsWithoutListenersAreIgnored() {
		assertDoesNotThrow(() -> EventBus.post(new SampleEvent("nobody listens")));
	}
}
