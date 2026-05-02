package me.hsgamer.extrastorage.hooks.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

import java.util.Optional;

public final class ExcellentEconomyHook extends WorthEconomyHook {
    private boolean setup = false;
    private ExcellentEconomyAPI api;

    public ExcellentEconomyHook() {
        if (this.isHooked()) {
            instance.getLogger().info("Using ExcellentEconomy as economy provider.");
        } else
            instance.getLogger().severe("Could not find dependency: ExcellentEconomy. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        if (Bukkit.getPluginManager().getPlugin("ExcellentEconomy") == null) {
            return false;
        }
        if (!setup) {
            RegisteredServiceProvider<ExcellentEconomyAPI> rsp = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
            api = (rsp != null) ? rsp.getProvider() : null;
            setup = true;
        }
        return api != null;
    }

    @Override
    protected boolean deposit(Player player, double price) {
        String cur = instance.getSetting().getCurrency();
        boolean hasCurrencySpecified = !cur.isEmpty();
        ExcellentCurrency currency = hasCurrencySpecified ? api.getCurrency(cur) : null;
        if (currency == null) {
            if (hasCurrencySpecified) {
                instance.getLogger().warning("The currency with ID '" + cur + "' could not be found! Using primary currency as default!");
            }
            Optional<ExcellentCurrency> optional = api.currencyRegistry().findPrimary();
            if (!optional.isPresent()) {
                return false;
            }
            currency = optional.get();
        }

        api.deposit(player, currency, price);
        return true;
    }
}
