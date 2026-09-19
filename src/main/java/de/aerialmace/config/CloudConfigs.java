package de.aerialmace.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Cloud config sharing backed by a Supabase (PostgREST) table. Anyone can upload the current
 * client config and download configs others shared.
 *
 * <p>Only the project's public anon key is used, which is safe to ship: the table needs Row
 * Level Security policies that allow anonymous {@code select} and {@code insert}. No private
 * service key, token, or user credential is ever embedded in the mod, and no local data is
 * uploaded unless the player explicitly presses upload.
 *
 * <p>Every call is asynchronous; results land in fields the client thread polls, so the game
 * never blocks on the network and offline play keeps working.
 */
public final class CloudConfigs {

    /** One shared config as listed by the cloud table. */
    public record Entry(long id, String name, String author, String createdAt) {
    }

    /** Upload / download / listing outcome used for the in-game status line. */
    public enum Status {
        IDLE, BUSY, OK, ERROR
    }

    private static final int MAX_BODY_BYTES = 512 * 1024;
    private static final String TABLE = "aerialmace_configs";
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    private static final AtomicReference<List<Entry>> ENTRIES = new AtomicReference<>(List.of());
    private static final AtomicReference<ModConfig> DOWNLOADED = new AtomicReference<>();
    private static final AtomicReference<String> MESSAGE = new AtomicReference<>("");
    private static volatile Status status = Status.IDLE;

    private CloudConfigs() {
    }

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES.get());
    }

    public static Status status() {
        return status;
    }

    public static String message() {
        return MESSAGE.get();
    }

    /** True once a project URL and the public anon key are configured. */
    public static boolean isConfigured(ModConfig config) {
        return config != null && isHttpsUrl(config.cloudShareUrl)
                && config.cloudShareKey != null && !config.cloudShareKey.isBlank();
    }

    /** Fetches the newest shared configs. Safe to call repeatedly; ignores overlapping calls. */
    public static void refresh(ModConfig config) {
        if (!requireConfigured(config) || status == Status.BUSY) {
            return;
        }
        setStatus(Status.BUSY, "Loading cloud configs...");
        send(config, request(config, "GET",
                "/rest/v1/" + TABLE + "?select=id,name,author,created_at&order=created_at.desc&limit=100", null))
                .thenAccept(response -> {
                    if (!isUsable(response)) {
                        setStatus(Status.ERROR, "Cloud request failed (" + statusCode(response) + ")");
                        return;
                    }
                    List<Entry> parsed = parseEntries(response.body());
                    ENTRIES.set(parsed);
                    setStatus(Status.OK, parsed.size() + " cloud configs");
                })
                .exceptionally(error -> {
                    setStatus(Status.ERROR, "Cloud unavailable");
                    return null;
                });
    }

    /** Uploads the current local config under the given name. */
    public static void upload(ModConfig config, String name) {
        if (!requireConfigured(config) || status == Status.BUSY) {
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        String safeName = trimmed.isBlank() ? "unnamed"
                : trimmed.substring(0, Math.min(48, trimmed.length()));
        setStatus(Status.BUSY, "Uploading config...");

        JsonObject payload = new JsonObject();
        payload.addProperty("name", safeName);
        payload.addProperty("author", config.cloudAuthor == null ? "" : config.cloudAuthor);
        payload.add("config", GSON.toJsonTree(config.sanitizedForSharing()));

        send(config, request(config, "POST", "/rest/v1/" + TABLE, GSON.toJson(payload)))
                .thenAccept(response -> {
                    if (!isUsable(response)) {
                        setStatus(Status.ERROR, "Upload failed (" + statusCode(response) + ")");
                        return;
                    }
                    setStatus(Status.OK, "Uploaded \"" + safeName + "\"");
                    refresh(config);
                })
                .exceptionally(error -> {
                    setStatus(Status.ERROR, "Upload unavailable");
                    return null;
                });
    }

    /** Downloads a shared config; the client thread applies it via {@link #poll(ModConfig)}. */
    public static void download(ModConfig config, long id) {
        if (!requireConfigured(config) || status == Status.BUSY) {
            return;
        }
        setStatus(Status.BUSY, "Downloading config...");
        send(config, request(config, "GET",
                "/rest/v1/" + TABLE + "?select=config&id=eq." + id + "&limit=1", null))
                .thenAccept(response -> {
                    if (!isUsable(response)) {
                        setStatus(Status.ERROR, "Download failed (" + statusCode(response) + ")");
                        return;
                    }
                    ModConfig applied = parseDownload(response.body(), config);
                    if (applied == null) {
                        setStatus(Status.ERROR, "Config could not be read");
                        return;
                    }
                    DOWNLOADED.set(applied);
                    setStatus(Status.OK, "Cloud config applied");
                })
                .exceptionally(error -> {
                    setStatus(Status.ERROR, "Download unavailable");
                    return null;
                });
    }

    /** Applies a finished download on the client thread. */
    public static boolean poll(ModConfig local) {
        ModConfig remote = DOWNLOADED.getAndSet(null);
        if (remote == null) {
            return false;
        }
        local.copyFrom(remote);
        local.normalize();
        ModConfig.requestSave(local);
        return true;
    }

    private static boolean requireConfigured(ModConfig config) {
        if (isConfigured(config)) {
            return true;
        }
        setStatus(Status.ERROR, "Cloud sharing not configured");
        return false;
    }

    private static void setStatus(Status newStatus, String message) {
        status = newStatus;
        MESSAGE.set(message);
    }

    private static int statusCode(HttpResponse<byte[]> response) {
        return response == null ? -1 : response.statusCode();
    }

    /** A response is usable when it succeeded and stayed within the size limit. */
    private static boolean isUsable(HttpResponse<byte[]> response) {
        return response != null
                && response.statusCode() >= 200 && response.statusCode() < 300
                && response.body() != null && response.body().length <= MAX_BODY_BYTES;
    }

    private static CompletableFuture<HttpResponse<byte[]>> send(ModConfig config, HttpRequest request) {
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpRequest request(ModConfig config, String method, String path, String body) {
        String base = config.cloudShareUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.cloudShareKey)
                .header("Authorization", "Bearer " + config.cloudShareKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal");
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        return builder.build();
    }

    private static List<Entry> parseEntries(byte[] body) {
        List<Entry> result = new ArrayList<>();
        try {
            JsonElement parsed = GSON.fromJson(new String(body, StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonArray()) {
                return result;
            }
            JsonArray array = parsed.getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                result.add(new Entry(
                        object.has("id") && !object.get("id").isJsonNull() ? object.get("id").getAsLong() : 0L,
                        object.has("name") && !object.get("name").isJsonNull() ? object.get("name").getAsString() : "unnamed",
                        object.has("author") && !object.get("author").isJsonNull() ? object.get("author").getAsString() : "",
                        object.has("created_at") && !object.get("created_at").isJsonNull() ? object.get("created_at").getAsString() : ""));
            }
        } catch (RuntimeException ignored) {
            // malformed response -> keep whatever we had
        }
        return result;
    }

    private static ModConfig parseDownload(byte[] body, ModConfig local) {
        try {
            JsonElement parsed = GSON.fromJson(new String(body, StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonArray() || parsed.getAsJsonArray().isEmpty()) {
                return null;
            }
            JsonElement first = parsed.getAsJsonArray().get(0);
            if (!first.isJsonObject() || !first.getAsJsonObject().has("config")) {
                return null;
            }
            JsonElement shared = first.getAsJsonObject().get("config");
            if (!shared.isJsonObject()) {
                return null;
            }
            JsonObject merged = GSON.toJsonTree(local).getAsJsonObject();
            for (var entry : shared.getAsJsonObject().entrySet()) {
                merged.add(entry.getKey(), entry.getValue());
            }
            // Cloud settings themselves stay local: a shared config cannot redirect the client.
            merged.addProperty("cloudShareUrl", local.cloudShareUrl);
            merged.addProperty("cloudShareKey", local.cloudShareKey);
            merged.addProperty("cloudConfigUrl", local.cloudConfigUrl);
            merged.addProperty("cloudSyncEnabled", local.cloudSyncEnabled);
            ModConfig result = GSON.fromJson(merged, ModConfig.class);
            if (result != null) {
                result.normalize();
            }
            return result;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isHttpsUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return "https".equalsIgnoreCase(URI.create(value.trim()).getScheme());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
