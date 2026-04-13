package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class WorthEconomyHook extends AbstractEconomyHook {
    @Override
    public int getAmount(ItemStack item) {
        if (!isHooked()) return 0;
        String key = ItemUtil.toMaterialKey(item);
        Worth worth = instance.getWorthManager().getWorth(key);
        return worth != null ? worth.getQuantity() : 0;
    }

    @Override
    protected double getRawPrice(Player player, ItemStack item, int amount) {
        String key = ItemUtil.toMaterialKey(item);
        Worth worth = instance.getWorthManager().getWorth(key);
        if (worth == null) return -1;

        return (worth.getPrice() / worth.getQuantity() * amount);
    }

    @Override
    protected boolean deposit(Player player, ItemStack item, int amount, double price) {
        return deposit(player, price);
    }

    protected abstract boolean deposit(Player player, double price);
}
