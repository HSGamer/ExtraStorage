package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.config.SettingConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

import java.util.Optional;

public final class ExcellentEconomyHook extends AbstractEconomyHook {
    private final ExcellentEconomyAPI api;
    private final ExcellentCurrency currency;
    private final boolean hooked;

    public ExcellentEconomyHook(ExtraStorage plugin) {
        super(plugin);
        RegisteredServiceProvider<ExcellentEconomyAPI> rsp = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
        ExcellentEconomyAPI api = (rsp != null) ? rsp.getProvider() : null;
        ExcellentCurrency currency = null;
        if (api != null) {
            String cur = plugin.get(SettingConfig.class).economy().currency();
            boolean hasCurrencySpecified = !cur.isEmpty();
            currency = hasCurrencySpecified ? api.getCurrency(cur) : null;
            if (currency == null) {
                if (hasCurrencySpecified) {
                    plugin.getLogger().warning("The currency with ID '" + cur + "' could not be found! Using primary currency as default!");
                }
                Optional<ExcellentCurrency> optional = api.currencyRegistry().findPrimary();
                if (optional.isPresent()) {
                    currency = optional.get();
                }
            }
        }
        this.api = api;
        this.currency = currency;
        this.hooked = api != null && currency != null;
    }

    @Override
    public boolean isHooked() {
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
    protected boolean deposit(Player player, double price) {
        api.deposit(player, currency, price);
        return true;
    }
}
