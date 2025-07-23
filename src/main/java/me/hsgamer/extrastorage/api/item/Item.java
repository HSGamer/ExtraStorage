package me.hsgamer.extrastorage.api.item;

import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

public interface Item {

    String getKey();

    boolean isLoaded();

    /**
     * Get the item type based on it's key
     *
     * @return the type of item
     */
    ItemUtil.ItemType getType();

    /**
     * Get the item based on it's key
     *
     * @return the ItemStack
     */
    ItemStack getItem();

    /**
     * Check if the item is filtered or not
     *
     * @return true if the item is in the filter, otherwise false
     */
    boolean isFiltered();

    /**
     * Change the item filter status
     *
     * @param status the status to be changed
     */
    void setFiltered(boolean status);

    /**
     * Get the current item quantity
     *
     * @return the item quantity
     */
    long getQuantity();

    /**
     * Add the item quantity
     *
     * @param quantity the quantity to be added
     * @see Item#subtract(long)
     * @see Item#set(long)
     */
    void add(long quantity);

    /**
     * Subtract the item quantity
     *
     * @param quantity the quantity to be subtracted
     * @see Item#add(long)
     * @see Item#set(long)
     */
    void subtract(long quantity);

    /**
     * Change the item quantity;
     *
     * @param quantity the item quantity to be changed
     * @see Item#add(long)
     * @see Item#subtract(long)
     */
    void set(long quantity);

}
