package de.aerialmace.friend;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

/** Single source of truth for friends. Modules must query this manager instead of own lists. */
public final class FriendManager {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aerialmace-friends.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, Friend> FRIENDS = new LinkedHashMap<>();

	private FriendManager() {
	}

	public static boolean add(String name) {
		return add(new Friend(name.trim(), null));
	}

	public static boolean add(Friend friend) {
		if (friend == null || friend.name().isBlank()) return false;
		String key = key(friend.name());
		if (FRIENDS.containsKey(key)) return false;
		FRIENDS.put(key, friend);
		return true;
	}

	public static boolean remove(String name) {
		return name != null && FRIENDS.remove(key(name)) != null;
	}

	public static boolean isFriend(PlayerEntity player) {
		return player != null && (isFriend(player.getName().getString()) || FRIENDS.values().stream()
				.anyMatch(friend -> friend.uuid() != null && friend.uuid().equals(player.getUuid())));
	}

	public static boolean isFriend(String name) {
		return name != null && FRIENDS.containsKey(key(name));
	}

	public static List<Friend> getFriends() {
		return Collections.unmodifiableList(new ArrayList<>(FRIENDS.values()));
	}

	public static void clear() {
		FRIENDS.clear();
	}

	public static void load() {
		FRIENDS.clear();
		if (!Files.exists(FILE)) return;
		try (Reader reader = Files.newBufferedReader(FILE)) {
			JsonArray array = GSON.fromJson(reader, JsonArray.class);
			if (array == null) return;
			for (var element : array) {
				if (!element.isJsonObject()) continue;
				JsonObject object = element.getAsJsonObject();
				String name = object.has("name") ? object.get("name").getAsString() : "";
				UUID uuid = object.has("uuid") && !object.get("uuid").isJsonNull()
						? UUID.fromString(object.get("uuid").getAsString()) : null;
				if (!name.isBlank()) add(new Friend(name, uuid));
			}
		} catch (IOException | RuntimeException ignored) {
			FRIENDS.clear();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			de.aerialmace.config.ConfigManager.copyToBackup(FILE, FILE.resolveSibling(FILE.getFileName() + ".bak"));
			JsonArray array = new JsonArray();
			for (Friend friend : FRIENDS.values()) {
				JsonObject object = new JsonObject();
				object.addProperty("name", friend.name());
				if (friend.uuid() != null) object.addProperty("uuid", friend.uuid().toString());
				array.add(object);
			}
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(array, writer);
			}
		} catch (IOException e) {
			de.aerialmace.debug.ClientLogger.warn("Could not save friends", e);
		}
	}

	private static String key(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}
}
