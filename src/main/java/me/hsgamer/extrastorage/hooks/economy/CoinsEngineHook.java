package me.hsgamer.extrastorage.hooks.economy;

import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import java.util.Optional;

public final class CoinsEngineHook extends WorthEconomyHook {

    public CoinsEngineHook() {
        if (this.isHooked()) {
            instance.getLogger().info("Using CoinsEngine as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "CoinsEngine"));
        } else instance.getLogger().severe("Could not find dependency: CoinsEngine. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        return instance.getServer().getPluginManager().isPluginEnabled("CoinsEngine");
    }

    @Override
    protected boolean deposit(Player player, ItemStack item, int amount, double price) {
        String cur = instance.getSetting().getCurrency();
        Currency currency;

        if (CoinsEngineAPI.hasCurrency(cur)) currency = CoinsEngineAPI.getCurrency(cur);
        else {
            instance.getLogger().warning("The currency with ID '" + cur + "' could not be found! Using Vault as default!");
            Optional<Currency> optional = CoinsEngineAPI.getCurrencyManager().getVaultCurrency();
            if (!optional.isPresent()) {
                return false;
            }
            currency = optional.get();
        }

        CoinsEngineAPI.addBalance(player, currency, price);
        return true;
    }
}
