package me.hsgamer.extrastorage.data.worth;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.configs.types.BukkitConfig;
import me.hsgamer.extrastorage.util.ItemUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorthManager
        extends BukkitConfig {

    private Map<String, Worth> cache;

    public WorthManager() {
        super("worth.yml");
    }

    @Override
    public void setup() {
        this.cache = new ConcurrentHashMap<>();

        for (String key : config.getConfigurationSection("Worth").getKeys(false)) {
            if (cache.containsKey(key)) continue;

            int quantity = config.getInt("Worth." + key + ".Quantity");
            double price = config.getDouble("Worth." + key + ".Price");
            if ((quantity < 1) || (price <= 0.0)) continue;

            key = ItemUtil.normalizeMaterialKey(key);
            cache.put(key, new ESWorth(key, quantity, price));
        }
    }

    public Worth getWorth(String key) {
        return cache.get(key);
    }

}
