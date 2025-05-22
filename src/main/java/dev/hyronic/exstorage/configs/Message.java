package dev.hyronic.exstorage.configs;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.configs.types.BukkitConfig;
import dev.hyronic.exstorage.util.Utils;

import java.util.HashMap;
import java.util.Map;

public final class Message
        extends BukkitConfig<ExtraStorage> {

    public static String PREFIX;

    public static final Map<String, String> messages = new HashMap<>();

    public Message() {
        super("messages.yml");
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

    public static String getMessage(String key) {
        return messages.getOrDefault(key, "Unknown message!");
    }

}
