package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NoneEconomyHook implements EconomyProvider {
    public NoneEconomyHook(ExtraStorage plugin) {
    }

    @Override
    public boolean isHooked() {
        return false;
    }

    @Override
    public int getAmount(ItemStack item) {
        return 0;
    }

    @Override
    public String getPrice(Player player, ItemStack item, int amount) {
        return null;
    }

    @Override
    public Result sellItem(Player player, ItemStack item, int amount) {
        return new Result(-1, -1, false);
    }
}
