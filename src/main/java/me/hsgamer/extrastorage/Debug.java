package me.hsgamer.extrastorage;

import java.util.logging.Level;

public class Debug {
    public static void log(String... messages) {
        ExtraStorage plugin = ExtraStorage.getInstance();
        if (plugin.getSetting().debug()) {
            for (String message : messages) {
                plugin.getLogger().info(message);
            }
        }
    }

    public static void log(String message, Throwable throwable) {
        ExtraStorage plugin = ExtraStorage.getInstance();
        if (plugin.getSetting().debug()) {
            plugin.getLogger().log(Level.INFO, message, throwable);
        }
    }
}
