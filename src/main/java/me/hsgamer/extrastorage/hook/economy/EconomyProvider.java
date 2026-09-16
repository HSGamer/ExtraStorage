package me.hsgamer.extrastorage.hook.economy;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface EconomyProvider {

    boolean isHooked();

    int getAmount(ItemStack item);

    String getPrice(Player player, ItemStack item, int amount);

    Result sellItem(Player player, ItemStack item, int amount);

    class Result {

        private final int amount;
        private final double price;
        private final boolean success;

        Result(int amount, double price, boolean success) {
            this.amount = amount;
            this.price = price;
            this.success = success;
        }

        public int getAmount() {
            return amount;
        }

        public double getPrice() {
            return price;
        }

        public boolean isSuccess() {
            return success;
        }

    }

}
