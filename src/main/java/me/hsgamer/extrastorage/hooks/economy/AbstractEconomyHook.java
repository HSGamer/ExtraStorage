package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public abstract class AbstractEconomyHook implements EconomyProvider {
    protected final ExtraStorage instance = ExtraStorage.getInstance();

    @Override
    public String getPrice(Player player, ItemStack item, int amount) {
        if (!isHooked()) return null;
        double price = getRawPrice(player, item, amount);
        if (price < 0) return null;
        return formatPrice(price);
    }

    @Override
    public void sellItem(Player player, ItemStack item, int amount, Consumer<Result> result) {
        if (!isHooked()) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        double price = getRawPrice(player, item, amount);
        if (price < 0) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, ItemUtil.toMaterialKey(item), amount, price);
        }

        boolean success = deposit(player, item, amount, price);
        result.accept(new Result(amount, price, success));
    }

    protected String formatPrice(double price) {
        return Digital.formatDouble("###,###.##", price);
    }

    protected abstract double getRawPrice(Player player, ItemStack item, int amount);

    protected abstract boolean deposit(Player player, ItemStack item, int amount, double price);
}
