package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.util.Digital;
import me.realized.tokenmanager.api.TokenManager;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class TokenManagerHook extends WorthEconomyHook {

    private final TokenManager api;

    public TokenManagerHook() {
        Plugin tmPlugin = Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
        api = (tmPlugin != null) ? (TokenManager) tmPlugin : null;

        if (this.isHooked()) {
            instance.getLogger().info("Using TokenManager as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "TokenManager"));
        } else
            instance.getLogger().severe("Could not find dependency: TokenManager. Please install it then try again!");
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
        return api.addTokens(player, (long) price);
    }
}
