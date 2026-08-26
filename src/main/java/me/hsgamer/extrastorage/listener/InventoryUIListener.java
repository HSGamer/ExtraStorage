package me.hsgamer.extrastorage.listener;

import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import io.github.projectunified.craftux.spigot.SpigotInventoryUIListener;
import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryHolder;

public class InventoryUIListener implements Loadable {
    private final ExtraStorage plugin;
    private SpigotInventoryUIListener listener;

    public InventoryUIListener(ExtraStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        listener = new SpigotInventoryUIListener(plugin);
        listener.register();
    }

    @Override
    public void disable() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof SpigotInventoryUI) {
                player.closeInventory();
            }
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }
}
