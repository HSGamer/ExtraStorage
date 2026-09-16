package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.manager.WorthManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public abstract class AbstractEconomyHook implements EconomyProvider {
    protected final ExtraStorage instance;

    protected AbstractEconomyHook(ExtraStorage instance) {
        this.instance = instance;
    }

    protected static Economy findVaultEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        return (rsp != null) ? rsp.getProvider() : null;
    }

    @Override
    public String getPrice(Player player, ItemStack item, int amount) {
        if (!isHooked()) return null;
        double price = getRawPrice(player, item, amount);
        if (price < 0) return null;
        return formatPrice(price);
    }

    @Override
    public Result sellItem(Player player, ItemStack item, int amount) {
        if (!isHooked()) {
            return new Result(-1, -1, false);
        }

        double price = getRawPrice(player, item, amount);
        if (price < 0) {
            return new Result(-1, -1, false);
        }

        if (instance.get(SettingConfig.class).log().sales()) {
            instance.get(Log.class).log(player, null, Log.Action.SELL, ItemUtil.toMaterialKey(item), amount, price);
        }

        onBeforeSell(player, item, amount, price);

        boolean success = deposit(player, price);
        return new Result(amount, price, success);
    }

    @Override
    public abstract int getAmount(ItemStack item);

    protected String formatPrice(double price) {
        return Digital.formatDouble("###,###.##", price);
    }

    protected void onBeforeSell(Player player, ItemStack item, int amount, double price) {
    }

    protected final Worth lookupWorth(ItemStack item) {
        return instance.get(WorthManager.class).getWorth(ItemUtil.toMaterialKey(item));
    }

    protected abstract double getRawPrice(Player player, ItemStack item, int amount);

    protected abstract boolean deposit(Player player, double price);
}
