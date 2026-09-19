package de.aerialmace.friend;

import java.util.UUID;

/** Persistent friend entry shared by combat and future visual modules. */
public record Friend(String name, UUID uuid) {
	public Friend {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Friend name must not be blank");
		}
	}
}
