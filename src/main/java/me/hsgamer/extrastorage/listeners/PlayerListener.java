package me.hsgamer.extrastorage.listeners;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class PlayerListener
        extends BaseListener {

    private final UserManager manager;

    public PlayerListener(ExtraStorage instance) {
        super(instance);
        this.manager = instance.getUserManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!manager.isLoaded())
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Please wait until the server is fully loaded!");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        manager.load(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        manager.save(uuid);
    }

}
