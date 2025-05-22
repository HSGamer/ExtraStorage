package dev.hyronic.exstorage.api;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.item.Worth;
import dev.hyronic.exstorage.api.user.User;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class StorageAPI {

    private static StorageAPI instance;

    public static StorageAPI getInstance() {
        if (instance == null) instance = new StorageAPI();
        return instance;
    }

    private final ExtraStorage main;

    private StorageAPI() {
        this.main = ExtraStorage.getInstance();
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
