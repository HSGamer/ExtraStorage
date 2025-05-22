package me.hsgamer.extrastorage.configs;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.configs.types.BukkitConfig;
import me.hsgamer.extrastorage.util.Utils;

import java.util.HashMap;
import java.util.Map;

public final class Message
        extends BukkitConfig<ExtraStorage> {

    public static final Map<String, String> messages = new HashMap<>();
    public static String PREFIX;

    public Message() {
        super("messages.yml");
    }

    public static String getMessage(String key) {
        return messages.getOrDefault(key, "Unknown message!");
    }

    @Override
    public void setup() {
        PREFIX = config.getString("PREFIX");

        messages.clear();
        config.getKeys(true).forEach(key -> {
            if (key.equals("PREFIX") || config.isConfigurationSection(key) || (!config.isString(key))) return;
            String val = config.getString(key).replaceAll(Utils.getRegex("prefix"), PREFIX);
            messages.put(key, val);
        });
    }

}
