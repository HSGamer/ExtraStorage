package me.hsgamer.extrastorage.listener;

import io.github.projectunified.minelib.plugin.listener.ListenerComponent;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.manager.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class PlayerListener implements ListenerComponent {

    private final ExtraStorage instance;
    private final UserManager manager;

    public PlayerListener(ExtraStorage instance) {
        this.instance = instance;
        this.manager = instance.get(UserManager.class);
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
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
