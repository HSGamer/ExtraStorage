package me.hsgamer.extrastorage.data.stub;

import com.google.common.base.Preconditions;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

public class StubItem implements Item {
    private final StubStorage storage;
    private final String key;
    private final io.github.projectunified.uniitem.api.Item item;

    public StubItem(StubStorage storage, String key) {
        this.storage = storage;
        this.key = key;
        this.item = ItemUtil.getItem(key);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean isLoaded() {
        return item.isValid();
    }

    @Override
    public ItemUtil.ItemType getType() {
        return ItemUtil.getItemType(item);
    }

    @Override
    public ItemStack getItem() {
        ItemStack itemStack = item.bukkitItem();
        Preconditions.checkNotNull(itemStack);
        return itemStack;
    }

    @Override
    public boolean isFiltered() {
        return storage.user.entry.getValue().items.get(key).filtered;
    }

    @Override
    public void setFiltered(boolean status) {
        storage.user.entry.setValue(u -> u.withItemModifiedIfFound(key, item -> item.withFiltered(status)));
    }

    @Override
    public long getQuantity() {
        return storage.user.entry.getValue().items.get(key).quantity;
    }

    @Override
    public void add(long quantity) {
        storage.user.entry.setValue(u -> u.withItemModifiedIfFound(key, item -> item.withQuantity(item.quantity + quantity)));
    }

    @Override
    public void subtract(long quantity) {
        storage.user.entry.setValue(u -> u.withItemModifiedIfFound(key, item -> item.withQuantity(item.quantity - quantity)));
    }

    @Override
    public void set(long quantity) {
        storage.user.entry.setValue(u -> u.withItemModifiedIfFound(key, item -> item.withQuantity(quantity)));
    }
}
