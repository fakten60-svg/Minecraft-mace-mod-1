package de.aerialmace.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Optional read-only cloud configuration sync. The client never sends credentials or local
 * settings to the remote URL. Invalid URLs, network failures, oversized responses, and malformed
 * JSON simply leave the local configuration untouched.
 */
public final class CloudConfigSync {
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final AtomicReference<ModConfig> READY = new AtomicReference<>();
    private static volatile boolean inFlight;

    private CloudConfigSync() {}

    public static void start(ModConfig config) {
        if (config.cloudSyncEnabled) request(config);
    }

    public static void request(ModConfig config) {
        if (!config.cloudSyncEnabled || !isAllowedUrl(config.cloudConfigUrl) || inFlight) return;
        inFlight = true;
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.cloudConfigUrl))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length > MAX_RESPONSE_BYTES) return null;
                    return parseOverlay(response.body(), config);
                }).exceptionally(ignored -> null)
                .thenAccept(ready -> { if (ready != null) READY.set(ready); inFlight = false; });
    }

    /** Applies a completed remote config on the client thread. */
    public static boolean poll(ModConfig local) {
        ModConfig remote = READY.getAndSet(null);
        if (remote == null) return false;
        local.copyFrom(remote);
        local.normalize();
        ModConfig.requestSave(local);
        return true;
    }

    public static boolean isAllowedUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try { return "https".equalsIgnoreCase(URI.create(value).getScheme()); }
        catch (IllegalArgumentException ignored) { return false; }
    }

    private static ModConfig parseOverlay(byte[] body, ModConfig local) {
        try {
            JsonElement parsed = GSON.fromJson(new String(body, java.nio.charset.StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) return null;
            JsonObject merged = GSON.toJsonTree(local).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) merged.add(entry.getKey(), entry.getValue());
            // The remote document may tune gameplay settings, but it can never redirect or
            // enable cloud sync itself; those controls remain local-only.
            merged.addProperty("cloudSyncEnabled", local.cloudSyncEnabled);
            merged.addProperty("cloudConfigUrl", local.cloudConfigUrl);
            ModConfig result = GSON.fromJson(merged, ModConfig.class);
            if (result != null) result.normalize();
            return result;
        } catch (RuntimeException ignored) { return null; }
    }
}
