package me.hsgamer.extrastorage.api;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.api.user.User;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class StorageAPI {

    private static StorageAPI instance;
    private final ExtraStorage main;

    private StorageAPI() {
        this.main = ExtraStorage.getInstance();
    }

    public static StorageAPI getInstance() {
        if (instance == null) instance = new StorageAPI();
        return instance;
    }

    public User getUser(UUID uuid) {
        return main.getUserManager().getUser(uuid);
    }

    public User getUser(OfflinePlayer player) {
        return this.getUser(player.getUniqueId());
    }

    public Worth getWorth(String key) {
        return main.getWorthManager().getWorth(key);
    }

}
