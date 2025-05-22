package dev.hyronic.exstorage.listeners;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.user.User;
import dev.hyronic.exstorage.data.user.ESUser;
import dev.hyronic.exstorage.data.user.UserManager;
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

        User user = manager.getUser(uuid);
        if (user == null) new ESUser(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        User user = manager.getUser(uuid);
        if (user == null) return;

        user.save();
    }

}
