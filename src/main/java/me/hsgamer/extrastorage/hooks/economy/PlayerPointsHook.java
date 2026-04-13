package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.util.Digital;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class PlayerPointsHook extends WorthEconomyHook {

    private final PlayerPointsAPI api;

    public PlayerPointsHook() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PlayerPoints");
        api = (plugin != null) ? ((PlayerPoints) plugin).getAPI() : null;

        if (this.isHooked()) {
            instance.getLogger().info("Using PlayerPoints as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "PlayerPoints"));
        } else
            instance.getLogger().severe("Could not find dependency: PlayerPoints. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        return (api != null);
    }

    @Override
    protected String formatPrice(double price) {
        return Digital.formatThousands((long) price);
    }

    @Override
    protected boolean deposit(Player player, ItemStack item, int amount, double price) {
        return api.give(player.getUniqueId(), (int) price);
    }
}
