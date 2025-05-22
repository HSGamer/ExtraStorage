package me.hsgamer.extrastorage.fetcher;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.util.UUIDTypeAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class UUIDFetcher {

    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final Cache<String, UUID> uuidCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private UUID id;

    private UUIDFetcher() {
    }

    /**
     * Fetches the uuid asynchronously and passes it to the consumer
     *
     * @param name   The name
     * @param action Do what you want to do with the uuid her
     */
    public static void getUUID(String name, Consumer<UUID> action) {
        getUUIDAt(name, action);
    }

    /**
     * Fetches the uuid synchronously and returns it
     *
     * @param name The name
     * @return The uuid
     */
    public static UUID getUUID(String name) {
        return getUUIDAt(name);
    }

    /**
     * Fetches the uuid synchronously for a specified name and time and passes the result to the consumer
     *
     * @param name   The name
     * @param action Do what you want to do with the uuid her
     */
    public static void getUUIDAt(String name, Consumer<UUID> action) {
        pool.execute(() -> action.accept(getUUIDAt(name)));
    }

    /**
     * Fetches the uuid synchronously for a specified name and time
     *
     * @param name The name
     */
    public static UUID getUUIDAt(String name) {
        UUID cached = uuidCache.getIfPresent(name);
        if (cached != null) return cached;

        try {
            name = name.toLowerCase();

            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(UUID_URL, name)).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            UUIDFetcher data = gson.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream())), UUIDFetcher.class);

            uuidCache.put(name, data.id);

            return data.id;
        } catch (Exception ignored) {
        }

        return null;
    }

}