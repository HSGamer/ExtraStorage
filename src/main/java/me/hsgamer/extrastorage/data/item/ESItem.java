package me.hsgamer.extrastorage.data.item;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

public final class ESItem
        implements Item {

    private final String key;
    private ItemUtil.ItemPair itemPair;
    private boolean filtered;
    private long quantity;

    public ESItem(String key, boolean filtered, long quantity) {
        key = ItemUtil.normalizeMaterialKey(key);
        this.key = key;
        this.filtered = filtered;
        this.quantity = quantity;
    }

    @Override
    public String getKey() {
        return key;
    }

    private void initItemPair() {
        if (itemPair == null) {
            itemPair = ItemUtil.getItem(key);
            if (itemPair.type() == ItemUtil.ItemType.NONE) {
                ExtraStorage.getInstance().getLogger().warning("Invalid item: " + key);
            }
        }
    }

    @Override
    public boolean isLoaded() {
        initItemPair();
        return itemPair.type() != ItemUtil.ItemType.NONE;
    }

    @Override
    public ItemUtil.ItemType getType() {
        initItemPair();
        return itemPair.type();
    }

    @Override
    public ItemStack getItem() {
        initItemPair();
        return itemPair.item().clone();
    }

    @Override
    public boolean isFiltered() {
        return filtered;
    }

    @Override
    public void setFiltered(boolean status) {
        this.filtered = status;
    }

    @Override
    public long getQuantity() {
        return quantity;
    }

    @Override
    public void add(long quantity) {
        this.quantity += quantity;
    }

    @Override
    public void subtract(long quantity) {
        this.quantity -= quantity;
    }

    @Override
    public void set(long quantity) {
        this.quantity = quantity;
    }

}
