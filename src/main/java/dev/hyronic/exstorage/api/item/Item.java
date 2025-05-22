package dev.hyronic.exstorage.api.item;

import dev.hyronic.exstorage.util.ItemUtil;
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
    int getQuantity();

    /**
     * Add the item quantity
     *
     * @param quantity the quantity to be added
     * @see Item#subtract(int)
     * @see Item#set(int)
     */
    void add(int quantity);

    /**
     * Subtract the item quantity
     *
     * @param quantity the quantity to be subtracted
     * @see Item#add(int)
     * @see Item#set(int)
     */
    void subtract(int quantity);

    /**
     * Change the item quantity;
     *
     * @param quantity the item quantity to be changed
     * @see Item#add(int)
     * @see Item#subtract(int)
     */
    void set(int quantity);

}
