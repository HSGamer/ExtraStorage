package me.hsgamer.extrastorage.fetcher;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TextureFetcher {
    private static final Gson gson = new Gson();
    private static final String URL = "https://playerdb.co/api/player/minecraft/%s";
    private static final Cache<String, TextureResponse> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private static TextureResponse getResponse(String name) {
        name = name.toLowerCase();
        TextureResponse cached = cache.getIfPresent(name);
        if (cached != null) {
            return cached;
        }

        try {
            String url = String.format(URL, name);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestProperty("User-Agent", "ExtraStorage/TextureFetcher");
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (InputStream inputStream = connection.getInputStream()) {
                JsonObject response = gson.fromJson(new InputStreamReader(inputStream), JsonObject.class);
                String code = response.get("code").getAsString();
                if (code.equalsIgnoreCase("player.found")) {
                    JsonObject data = response.get("data").getAsJsonObject();
                    JsonObject player = data.get("player").getAsJsonObject();
                    UUID uuid = UUID.fromString(player.get("id").getAsString());
                    String textureUrl = player.get("skin_texture").getAsString();
                    TextureResponse textureResponse = new TextureResponse(uuid, textureUrl);
                    cache.put(name, textureResponse);
                    return textureResponse;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static UUID getUUID(String name) {
        TextureResponse response = getResponse(name);
        return response != null ? response.uuid : null;
    }

    public static String getTextureUrl(String name) {
        TextureResponse response = getResponse(name);
        return response != null ? response.textureUrl : null;
    }

    private static class TextureResponse {
        private final UUID uuid;
        private final String textureUrl;

        private TextureResponse(UUID uuid, String textureUrl) {
            this.uuid = uuid;
            this.textureUrl = textureUrl;
        }
    }
}
