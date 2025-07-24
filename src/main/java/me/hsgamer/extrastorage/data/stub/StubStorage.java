package me.hsgamer.extrastorage.data.stub;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.ItemImpl;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;

import java.util.*;
import java.util.stream.Collectors;

public class StubStorage implements Storage {
    private static final ExtraStorage instance = ExtraStorage.getInstance();
    final StubUser user;

    public StubStorage(StubUser user) {
        this.user = user;
    }

    @Override
    public boolean getStatus() {
        return user.entry.getValue().status;
    }

    @Override
    public void setStatus(boolean status) {
        user.entry.setValue(u -> u.withStatus(status));
    }

    @Override
    public long getSpace() {
        long space = user.entry.getValue().space;
        if ((instance.getSetting().getMaxSpace() == -1) || user.hasPermission(Constants.STORAGE_UNLIMITED_PERMISSION) || (space < 0))
            return -1;
        return Digital.getBetween(0, Long.MAX_VALUE, space);
    }

    @Override
    public void setSpace(long space) {
        user.entry.setValue(u -> u.withSpace(space));
    }

    @Override
    public void addSpace(long space) {
        user.entry.setValue(u -> u.withSpace(u.space + space));
    }

    @Override
    public long getUsedSpace() {
        long usedSpace = 0;
        for (Map.Entry<String, ItemImpl> entry : user.entry.getValue().items.entrySet()) {
            if (!ItemUtil.isValidItem(entry.getKey())) {
                continue;
            }
            try {
                usedSpace = Math.addExact(usedSpace, entry.getValue().quantity);
            } catch (ArithmeticException e) {
                // If the used space exceeds Long.MAX_VALUE, we return Long.MAX_VALUE
                return Long.MAX_VALUE;
            }
        }
        return usedSpace;
    }

    @Override
    public long getFreeSpace() {
        long space = this.getSpace();
        if (space < 0) return -1;
        return (space - this.getUsedSpace());
    }

    @Override
    public boolean isMaxSpace() {
        long free = this.getFreeSpace();
        if (free == -1) return false;
        return (free < 1);
    }

    @Override
    public double getSpaceAsPercent(boolean order) {
        long space = this.getSpace();
        if (space < 0) return -1;

        double percent = (double) getUsedSpace() / space * 100;
        return Digital.getBetween(0.0, 100.0, Digital.formatDouble(order ? percent : (100.0 - percent)));
    }

    @Override
    public boolean canStore(Object key) {
        if (!user.entry.getValue().status) return false;
        String validKey = ItemUtil.toMaterialKey(key);
        ItemImpl item = user.entry.getValue().items.get(validKey);
        return (item != null) && item.filtered;
    }

    @Override
    public Map<String, Item> getFilteredItems() {
        return user.entry.getValue().items.entrySet()
                .stream()
                .filter(entry -> entry.getValue().filtered)
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), new StubItem(this, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Item> getUnfilteredItems() {
        return user.entry.getValue().items.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().filtered)
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), new StubItem(this, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Item> getItems() {
        return user.entry.getValue().items.keySet()
                .stream()
                .map(item -> new AbstractMap.SimpleEntry<>(item, new StubItem(this, item)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Optional<Item> getItem(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        return user.entry.getValue().items.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(validKey))
                .findFirst()
                .map(entry -> new StubItem(this, entry.getKey()));
    }

    @Override
    public void addNewItem(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        user.entry.setValue(u -> u.withItemIfNotFound(validKey, ItemImpl.EMPTY.withFiltered(true).withQuantity(0)));
    }

    @Override
    public void unfilter(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        user.entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> i.withFiltered(false)));
    }

    @Override
    public void add(Object key, long quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        user.entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> i.withQuantity(i.quantity + quantity)));
    }

    @Override
    public void subtract(Object key, long quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        user.entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> {
            long newQuantity = i.quantity - quantity;
            if (newQuantity < 0) return null;
            return i.withQuantity(newQuantity);
        }));
    }

    @Override
    public void set(Object key, long quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        user.entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> {
            if (quantity < 0) return null;
            return i.withQuantity(quantity);
        }));
    }

    @Override
    public void reset(Object key) {
        if (key != null) this.set(key, 0);
        else {
            user.entry.setValue(u -> {
                Map<String, ItemImpl> items = new HashMap<>(user.entry.getValue().items);
                Iterator<Map.Entry<String, ItemImpl>> iterator = items.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, ItemImpl> entry = iterator.next();
                    if (entry.getValue().quantity < 1 && !entry.getValue().filtered) {
                        iterator.remove();
                    } else {
                        entry.setValue(entry.getValue().withQuantity(0));
                    }
                }
                return u.withItems(items);
            });
        }
    }
}
