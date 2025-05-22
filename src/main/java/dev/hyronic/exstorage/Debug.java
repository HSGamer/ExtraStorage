package dev.hyronic.exstorage;

import org.bukkit.plugin.java.JavaPlugin;

public class Debug {
    public static boolean enabled = false;

    public static void log(String... messages) {
        if (enabled) {
            JavaPlugin plugin = ExtraStorage.getInstance();
            for (String message : messages) {
                plugin.getLogger().info(message);
            }
        }
    }
}
