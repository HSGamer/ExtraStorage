package me.hsgamer.extrastorage.api.storage;

import me.hsgamer.extrastorage.api.item.Item;

import java.util.Map;
import java.util.Optional;

public interface Storage {

    /**
     * Get the storage usage status
     *
     * @return true if the player is still using the storage, otherwise false
     */
    boolean getStatus();

    /**
     * Change the storage usage status
     *
     * @param status the status to be changed
     */
    void setStatus(boolean status);

    /**
     * Get the storage space
     *
     * @return the storage space
     * @see Storage#getUsedSpace()
     * @see Storage#getFreeSpace()
     * @see Storage#getSpaceAsPercent(boolean)
     */
    long getSpace();

    /**
     * Change the storage space
     *
     * @param space the amount of space to be changed
     * @see Storage#addSpace(long)
     */
    void setSpace(long space);

    /**
     * Increase the storage space
     *
     * @param space the amount of space to be added
     * @see Storage#setSpace(long)
     */
    void addSpace(long space);

    /**
     * Get the total used space
     *
     * @return the total used space
     * @see Storage#getSpace()
     * @see Storage#getFreeSpace()
     * @see Storage#getSpaceAsPercent(boolean)
     */
    long getUsedSpace();

    /**
     * Get free storage space
     *
     * @return the remaining storage space, or -1 if unlimited
     * @see Storage#getSpace()
     * @see Storage#getUsedSpace()
     * @see Storage#getSpaceAsPercent(boolean)
     */
    long getFreeSpace();

    /**
     * Check if the storage is full
     *
     * @return true if the storage is full, otherwise false
     */
    boolean isMaxSpace();

    /**
     * Get the total used space (or free space) as percent
     *
     * @param order true if you want to follow the order from 1% to 100%
     * @return the percentage
     * @see Storage#getSpace()
     * @see Storage#getUsedSpace()
     * @see Storage#getFreeSpace()
     */
    double getSpaceAsPercent(boolean order);

    /**
     * Check if the specified item can be stored or not
     *
     * @param key the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @return true if the specified item can be stored, otherwise false
     */
    boolean canStore(Object key);

    /**
     * Get all items are not in the filter
     *
     * @return the Map contains all items are not in the filter
     * @see Storage#getFilteredItems()
     * @see Storage#getItems()
     */
    Map<String, Item> getUnfilteredItems();

    /**
     * Get all items are in the filter
     *
     * @return the Map contains all items in the filter
     * @see Storage#getUnfilteredItems()
     * @see Storage#getItems()
     */
    Map<String, Item> getFilteredItems();

    /**
     * Get all items are in the storage
     *
     * @return HashMap contains all items in the storage
     * @see Storage#getFilteredItems()
     * @see Storage#getUnfilteredItems()
     */
    Map<String, Item> getItems();

    /**
     * Get the specified item in storage
     *
     * @param key the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @return the {@link Optional Optional&#60;Item&#62;}
     */
    Optional<Item> getItem(Object key);

    /**
     * Add new item to the storage
     *
     * @param key the item key. Can be an ItemStack or a string as MATERIAL:DATA
     */
    void addNewItem(Object key);

    /**
     * <p>Unfilter the specified item. If the quantity of that item is less than 1, it will be removed from the storage,
     * otherwise, it still in the storage until it is withdrawn all of them.</p>
     * <p>To make the item to be filtered (if and only if that item still in the storage),
     * please take a look at: {@link Item#setFiltered(boolean) Item#setFiltered(boolean)},
     * otherwise, you have to use {@link Storage#addNewItem(Object) Storage#addNewItem(Object)}</p>
     *
     * @param key the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @see Storage#addNewItem(Object)
     * @see Item#setFiltered(boolean)
     */
    void unfilter(Object key);

    /**
     * Add the item quantity
     *
     * @param key      the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param quantity the quantity to be added
     * @see Storage#subtract(Object, int)
     * @see Storage#set(Object, int)
     * @see Storage#reset(Object)
     */
    void add(Object key, int quantity);

    /**
     * Subtract the item quantity. For unfiltered items, if the quantity is less than 1 after subtracted,
     * it will be automatically removed from the storage.
     *
     * @param key      the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param quantity the quantity to be subtracted
     * @see Storage#add(Object, int)
     * @see Storage#set(Object, int)
     * @see Storage#reset(Object)
     */
    void subtract(Object key, int quantity);

    /**
     * Set the item quantity. And same as {@link Storage#subtract(Object, int) subtract(Object, int)} method,
     * for unfiltered items, if the quantity is set less than 1, it will be automatically removed from the storage.
     *
     * @param key      the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param quantity the quantity to be set
     * @see Storage#add(Object, int)
     * @see Storage#subtract(Object, int)
     * @see Storage#reset(Object)
     */
    void set(Object key, int quantity);

    /**
     * Reset the item quantity (can be null to reset all items). And same as {@link Storage#subtract(Object, int) subtract(Object, int)} method,
     * for unfiltered items, after reseting, it will be automatically removed from the storage.
     *
     * @param key the item key. Can be an ItemStack, a string as MATERIAL:DATA or null for all items
     * @see Storage#add(Object, int)
     * @see Storage#subtract(Object, int)
     * @see Storage#set(Object, int)
     */
    void reset(Object key);

}
