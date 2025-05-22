package me.hsgamer.extrastorage.data.user;

import com.google.gson.JsonObject;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.item.ESItem;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EStorage
        implements Storage {

    private static final ExtraStorage instance = ExtraStorage.getInstance();
    final Map<String, Item> items;
    /*
     * Các biến hỗ trợ khác:
     */
    private final User user;
    /*
     * Không gian lưu trữ:
     */
    long space;
    /*
     * Trạng thái kho chứa:
     */
    private boolean status;

    // Sử dụng hàm này cho việc tải dữ liệu đã có sẵn:
    EStorage(User user, boolean status, long space, JsonObject items, JsonObject unused) {
        this.user = user;
        this.items = new ConcurrentHashMap<>();

        this.status = status;
        this.space = space;

        if (items != null) items.entrySet().forEach(entry -> {
            String key = ItemUtil.normalizeMaterialKey(entry.getKey());
            int quantity = entry.getValue().getAsInt();

            Item esItem = new ESItem(key, true, quantity);
            this.items.put(key, esItem);
        });

        if (unused != null) unused.entrySet().forEach(entry -> {
            String key = ItemUtil.normalizeMaterialKey(entry.getKey());
            int quantity = entry.getValue().getAsInt();

            Item esItem = new ESItem(key, false, quantity);
            this.items.put(key, esItem);
        });

        // TODO: Temporary fix for missing items
        for (String key : instance.getSetting().getWhitelist()) {
            key = ItemUtil.normalizeMaterialKey(key);
            if (this.items.containsKey(key)) continue;
            Item esItem = new ESItem(key, true, 0);
            this.items.put(key, esItem);
        }
    }

    // Sử dụng hàm này cho việc tạo mới dữ liệu:
    EStorage(User user) {
        this.user = user;
        this.items = new ConcurrentHashMap<>();

        this.status = true;
        this.space = instance.getSetting().getMaxSpace();

        for (String key : instance.getSetting().getWhitelist()) {
            key = ItemUtil.normalizeMaterialKey(key);
            Item esItem = new ESItem(key, true, 0);
            items.put(key, esItem);
        }
    }

    @Override
    public boolean getStatus() {
        return status;
    }

    @Override
    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public long getSpace() {
        if ((instance.getSetting().getMaxSpace() == -1) || user.hasPermission(Constants.STORAGE_UNLIMITED_PERMISSION) || (space < 0))
            return -1;
        return Digital.getBetween(0, Long.MAX_VALUE, space);
    }

    @Override
    public void setSpace(long space) {
        this.space = space;
    }

    @Override
    public void addSpace(long space) {
        this.space += space;
    }

    @Override
    public long getUsedSpace() {
        return items.values().stream().filter(Item::isLoaded).mapToLong(Item::getQuantity).sum();
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
        if (!status) return false;
        String validKey = ItemUtil.toMaterialKey(key);
        Item item = items.get(validKey);
        return ((item != null) && item.isFiltered());
    }

    /*
     * Các vật phẩm được lưu trữ:
     */
    @Override
    public Map<String, Item> getUnfilteredItems() {
        Map<String, Item> newMap = new HashMap<>();
        for (Map.Entry<String, Item> entry : items.entrySet()) {
            if (!entry.getValue().isFiltered()) newMap.put(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    @Override
    public Map<String, Item> getFilteredItems() {
        Map<String, Item> newMap = new HashMap<>();
        for (Map.Entry<String, Item> entry : items.entrySet()) {
            if (entry.getValue().isFiltered()) newMap.put(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    @Override
    public Map<String, Item> getItems() {
        return new HashMap<>(items);
    }

    @Override
    public Optional<Item> getItem(Object key) {
        return Optional.ofNullable(items.get(ItemUtil.toMaterialKey(key)));
    }

    @Override
    public void addNewItem(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        items.putIfAbsent(validKey, new ESItem(validKey, true, 0));
    }

    @Override
    public void unfilter(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        Item item = items.get(validKey);
        if (item == null) return;
        item.setFiltered(false);
    }

    @Override
    public void add(Object key, int quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        items.get(validKey).add(quantity);
    }

    @Override
    public void subtract(Object key, int quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        Item item = items.get(validKey);
        item.subtract(quantity);
        if ((item.getQuantity() < 1) && (!item.isFiltered())) items.remove(validKey);
    }


    /*
     * Hàm khởi tạo:
     */

    @Override
    public void set(Object key, int quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        Item item = items.get(validKey);
        item.set(quantity);
        if ((item.getQuantity() < 1) && (!item.isFiltered())) items.remove(validKey);
    }

    @Override
    public void reset(Object key) {
        if (key != null) this.set(key, 0);
        else {
            List<Item> values = new ArrayList<>(items.values());
            for (Item item : values) {
                if (item.isFiltered()) item.set(0);
                else items.remove(item.getKey());
            }
        }
    }

}
