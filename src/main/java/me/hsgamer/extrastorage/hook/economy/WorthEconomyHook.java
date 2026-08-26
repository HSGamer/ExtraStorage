package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.manager.WorthManager;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class WorthEconomyHook extends AbstractEconomyHook {
    public WorthEconomyHook(ExtraStorage plugin) {
        super(plugin);
    }

    @Override
    public int getAmount(ItemStack item) {
        if (!isHooked()) return 0;
        String key = ItemUtil.toMaterialKey(item);
        Worth worth = instance.get(WorthManager.class).getWorth(key);
        return worth != null ? worth.getQuantity() : 0;
    }

    @Override
    protected double getRawPrice(Player player, ItemStack item, int amount) {
        String key = ItemUtil.toMaterialKey(item);
        Worth worth = instance.get(WorthManager.class).getWorth(key);
        if (worth == null) return -1;

        return (worth.getPrice() / worth.getQuantity() * amount);
    }

    @Override
    protected boolean deposit(Player player, ItemStack item, int amount, double price) {
        return deposit(player, price);
    }

    protected abstract boolean deposit(Player player, double price);
}
