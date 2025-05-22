package dev.hyronic.exstorage.data.item;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.item.Item;
import dev.hyronic.exstorage.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

public final class ESItem
        implements Item {

    private final String key;

    @Override
    public String getKey() {
        return key;
    }

    private ItemUtil.ItemPair itemPair;

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


    private boolean filtered;

    @Override
    public boolean isFiltered() {
        return filtered;
    }

    @Override
    public void setFiltered(boolean status) {
        this.filtered = status;
    }


    private int quantity;

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void add(int quantity) {
        this.quantity += quantity;
    }

    @Override
    public void subtract(int quantity) {
        this.quantity -= quantity;
    }

    @Override
    public void set(int quantity) {
        this.quantity = quantity;
    }


    public ESItem(String key, boolean filtered, int quantity) {
        key = ItemUtil.normalizeMaterialKey(key);
        this.key = key;
        this.filtered = filtered;
        this.quantity = quantity;
    }

}
