package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;

import me.hsgamer.extrastorage.util.Digital;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PlayerPointsHook extends WorthEconomyHook {

    private final PlayerPointsAPI api;

    public PlayerPointsHook(ExtraStorage plugin) {
        super(plugin);
        Plugin ppPlugin = Bukkit.getServer().getPluginManager().getPlugin("PlayerPoints");
        api = (ppPlugin != null) ? ((PlayerPoints) ppPlugin).getAPI() : null;

        if (this.isHooked()) {
            instance.getLogger().info("Using PlayerPoints as economy provider.");
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
    protected boolean deposit(Player player, double price) {
        return api.give(player.getUniqueId(), (int) price);
    }
}
