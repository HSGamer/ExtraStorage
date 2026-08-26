package me.hsgamer.extrastorage;

import me.hsgamer.extrastorage.config.SettingConfig;

import java.util.logging.Level;

public class Debug {
    public static void log(String... messages) {
        ExtraStorage plugin = ExtraStorage.getInstance();
        if (plugin.get(SettingConfig.class).debug()) {
            for (String message : messages) {
                plugin.getLogger().info(message);
            }
        }
    }

    public static void log(String message, Throwable throwable) {
        ExtraStorage plugin = ExtraStorage.getInstance();
        if (plugin.get(SettingConfig.class).debug()) {
            plugin.getLogger().log(Level.INFO, message, throwable);
        }
    }
}
