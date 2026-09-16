package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.util.Digital;
import me.realized.tokenmanager.api.TokenManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class TokenManagerHook extends AbstractEconomyHook {

    private TokenManager api;
    private boolean hooked;
    private boolean setup;

    public TokenManagerHook(ExtraStorage plugin) {
        super(plugin);
    }

    @Override
    public boolean isHooked() {
        if (!setup) {
            Plugin tmPlugin = Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
            api = (tmPlugin != null) ? (TokenManager) tmPlugin : null;
            hooked = api != null;
            setup = true;
        }
        return hooked;
    }

    @Override
    public int getAmount(ItemStack item) {
        if (!isHooked()) return 0;
        Worth worth = lookupWorth(item);
        return worth != null ? worth.getQuantity() : 0;
    }

    @Override
    protected double getRawPrice(Player player, ItemStack item, int amount) {
        Worth worth = lookupWorth(item);
        if (worth == null) return -1;
        return worth.getPrice() / worth.getQuantity() * amount;
    }

    @Override
    protected String formatPrice(double price) {
        return Digital.formatThousands((long) price);
    }

    @Override
    protected boolean deposit(Player player, double price) {
        return api.addTokens(player, (long) price);
    }
}
