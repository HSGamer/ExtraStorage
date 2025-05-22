package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public interface EconomyProvider {

    String NOT_SUPPORTED_MSG = "This feature has not been supported yet!";

    ExtraStorage instance = ExtraStorage.getInstance();

    default boolean isHooked() {
        throw new IllegalArgumentException(NOT_SUPPORTED_MSG);
    }

    default int getAmount(ItemStack item) {
        throw new IllegalArgumentException(NOT_SUPPORTED_MSG);
    }

    default String getPrice(Player player, ItemStack item, int amount) {
        throw new IllegalArgumentException(NOT_SUPPORTED_MSG);
    }

    default void sellItem(Player player, ItemStack item, int amount, Consumer<Result> result) {
        throw new IllegalArgumentException(NOT_SUPPORTED_MSG);
    }

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
