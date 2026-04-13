package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class NoneEconomyHook implements EconomyProvider {
    public NoneEconomyHook() {
        ExtraStorage.getInstance().getLogger().info("Using no economy provider.");
    }

    @Override
    public boolean isHooked() {
        return true;
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
    public void sellItem(Player player, ItemStack item, int amount, Consumer<Result> result) {
        result.accept(new Result(-1, -1, false));
    }
}
