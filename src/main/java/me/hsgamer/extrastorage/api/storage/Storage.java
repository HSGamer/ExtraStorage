package me.hsgamer.extrastorage.api.storage;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;

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
     * Consume as much of the stack as fits into the storage, store it and notify.
     * <p>
     * The stack is split against the current free space:
     * <ul>
     *     <li>the whole stack fits → {@code onFullStore} is run and the full {@code amount} is stored;</li>
     *     <li>only part of the stack fits → {@code onResidual} receives the leftover amount that stays
     *     in the world, and the storable part is stored;</li>
     *     <li>the storage is full → {@code 0} is returned, nothing is stored and neither handler runs.</li>
     * </ul>
     * The stored amount is added via {@link #add(Object, long)} and {@code onAdded} is notified
     * with the item in the storage and the stored amount.
     *
     * @param key         the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param amount      the total amount of the stack
     * @param onResidual  receives the amount that stays in the world when only part of the stack fits
     * @param onFullStore run when the whole stack fits into the storage
     * @param onAdded     receives the item in the storage and the stored amount, after the add
     * @return the amount stored, or 0 when the storage is full (nothing is stored, neither handler runs)
     */
    default int consumeStack(Object key, int amount, IntConsumer onResidual, Runnable onFullStore, BiConsumer<Item, Integer> onAdded) {
        return consumeStack(key, amount, this.getFreeSpace(), onResidual, onFullStore, onAdded);
    }

    /**
     * Consume as much of the stack as fits into the given free space, store it and notify.
     * <p>
     * Same as {@link #consumeStack(Object, int, IntConsumer, Runnable, BiConsumer)} but with a precomputed
     * free space, so multiple stacks can be consumed against a single {@link #getFreeSpace()} call.
     * The caller is responsible for tracking the remaining free space.
     *
     * @param key         the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param amount      the total amount of the stack
     * @param freeSpace   the free space of the storage, or -1 if unlimited
     * @param onResidual  receives the amount that stays in the world when only part of the stack fits
     * @param onFullStore run when the whole stack fits into the storage
     * @param onAdded     receives the item in the storage and the stored amount, after the add; may be {@code null}
     * @return the amount stored, or 0 when the storage is full (nothing is stored, neither handler runs)
     */
    default int consumeStack(Object key, int amount, long freeSpace, IntConsumer onResidual, Runnable onFullStore, BiConsumer<Item, Integer> onAdded) {
        int store;
        if (freeSpace == -1) {
            store = amount;
        } else if (freeSpace < 1) {
            return 0;
        } else {
            store = (int) Math.min(freeSpace, amount);
        }

        if (store < amount) {
            onResidual.accept(amount - store);
        } else {
            onFullStore.run();
        }
        add(key, store);
        if (onAdded != null) {
            getItem(key).ifPresent(added -> onAdded.accept(added, store));
        }
        return store;
    }

    /**
     * Consume the given items into the storage, storing each whole stack or the part that fits.
     * <p>
     * The free space is computed once and tracked across the items.
     * Fully stored items are removed from the collection, so it must support element removal.
     * Stops at the first item that does not fully fit, leaving the residual in that item's stack.
     *
     * @param collection  the items to consume; fully stored items are removed from the collection
     * @param stackGetter gets the item stack of each element; may be {@code null} if the elements are item stacks
     * @param filter      the stacks to consume; stacks that fail the filter are skipped
     * @param onFullStore receives each element that is fully stored, so the caller can remove it elsewhere;
     *                    may be {@code null} if no external removal is needed
     * @param onAdded     receives the item in the storage and the stored amount, after each add
     * @param <T>         the element type
     * @return the total amount stored
     */
    default <T> int consumeStack(Collection<T> collection, Function<T, ItemStack> stackGetter, Predicate<ItemStack> filter, Consumer<T> onFullStore, BiConsumer<Item, Integer> onAdded) {
        int count = 0;
        long freeSpace = this.getFreeSpace();
        Iterator<T> iterator = collection.iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            ItemStack stack = stackGetter != null ? stackGetter.apply(element) : (ItemStack) element;
            if (ItemUtil.isAir(stack)) continue;
            if (!filter.test(stack)) continue;

            Optional<Item> optional = getItem(stack);
            if (!optional.isPresent()) continue;
            if (!optional.get().isLoaded()) continue;

            int amount = stack.getAmount();
            int store = consumeStack(stack, amount, freeSpace, stack::setAmount, () -> {
                if (onFullStore != null) onFullStore.accept(element);
                iterator.remove();
            }, onAdded);
            count += store;
            if (freeSpace != -1) freeSpace -= store;
            if (store < amount) break;
        }
        return count;
    }

    /**
     * Consume the given item stacks into the storage, storing each whole stack or the part that fits.
     * <p>
     * Fully stored stacks are removed from the iterator and {@code onFullStore} is called for each,
     * so the caller can remove them elsewhere.
     *
     * @param stacks      the item stacks to consume; fully stored stacks are removed from the iterator
     * @param filter      the stacks to consume; stacks that fail the filter are skipped
     * @param onFullStore receives each stack that is fully stored into the storage
     * @param onAdded     receives the item in the storage and the stored amount, after each add
     * @return the total amount stored
     */
    default int consumeStack(Collection<ItemStack> stacks, Predicate<ItemStack> filter, Consumer<ItemStack> onFullStore, BiConsumer<Item, Integer> onAdded) {
        return consumeStack(stacks, null, filter, onFullStore, onAdded);
    }

    /**
     * Consume the given dropped item entities into the storage, storing each whole stack or the part that fits.
     * <p>
     * Fully stored entities are removed from the world and from the iterator.
     *
     * @param entities the dropped item entities to consume; fully stored entities are removed from the iterator
     * @param filter   the entities to consume; entities that fail the filter are skipped
     * @param onAdded  receives the item in the storage and the stored amount, after each add
     * @return the total amount stored
     */
    default int consumeStack(Collection<org.bukkit.entity.Item> entities, Predicate<ItemStack> filter, BiConsumer<Item, Integer> onAdded) {
        return consumeStack(entities, org.bukkit.entity.Item::getItemStack, filter, org.bukkit.entity.Item::remove, onAdded);
    }

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
     * @see Storage#subtract(Object, long)
     * @see Storage#set(Object, long)
     * @see Storage#reset(Object)
     */
    void add(Object key, long quantity);

    /**
     * Subtract the item quantity. For unfiltered items, if the quantity is less than 1 after subtracted,
     * it will be automatically removed from the storage.
     *
     * @param key      the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param quantity the quantity to be subtracted
     * @see Storage#add(Object, long)
     * @see Storage#set(Object, long)
     * @see Storage#reset(Object)
     */
    void subtract(Object key, long quantity);

    /**
     * Set the item quantity. And same as {@link Storage#subtract(Object, long) subtract(Object, long)} method,
     * for unfiltered items, if the quantity is set less than 1, it will be automatically removed from the storage.
     *
     * @param key      the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @param quantity the quantity to be set
     * @see Storage#add(Object, long)
     * @see Storage#subtract(Object, long)
     * @see Storage#reset(Object)
     */
    void set(Object key, long quantity);

    /**
     * Reset the item quantity (can be null to reset all items). And same as {@link Storage#subtract(Object, long) subtract(Object, long)} method,
     * for unfiltered items, after reseting, it will be automatically removed from the storage.
     *
     * @param key the item key. Can be an ItemStack, a string as MATERIAL:DATA or null for all items
     * @see Storage#add(Object, long)
     * @see Storage#subtract(Object, long)
     * @see Storage#set(Object, long)
     */
    void reset(Object key);

}
