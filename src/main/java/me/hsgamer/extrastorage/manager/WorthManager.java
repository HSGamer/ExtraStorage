package me.hsgamer.extrastorage.manager;

import io.github.projectunified.craftconfig.bukkit.BukkitConfig;
import io.github.projectunified.craftconfig.common.ConfigNode;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.component.Reloadable;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.data.worth.ESWorth;
import me.hsgamer.extrastorage.util.ItemUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorthManager implements Reloadable {

    private final BukkitConfig bukkitConfig;
    private Map<String, Worth> cache;

    public WorthManager(ExtraStorage plugin) {
        this.bukkitConfig = new BukkitConfig(plugin, "worth.yml");
        this.bukkitConfig.setup();
        this.load();
    }

    @Override
    public void reload() {
        this.bukkitConfig.reload();
        this.load();
    }

    private void load() {
        this.cache = new ConcurrentHashMap<>();
        ConfigNode worthSection = bukkitConfig.node("Worth");
        if (!worthSection.exists() || !worthSection.hasChild()) return;

        for (Map.Entry<String, ConfigNode> entry : worthSection.getChildren().entrySet()) {
            String key = entry.getKey();
            key = ItemUtil.normalizeMaterialKey(key);
            if (cache.containsKey(key)) continue;

            ConfigNode value = entry.getValue();
            int quantity = value.node("Quantity").get(int.class, -1);
            double price = value.node("Price").get(double.class, 0.0);
            if ((quantity < 1) || (price <= 0.0)) continue;

            cache.put(key, new ESWorth(key, quantity, price));
        }
    }

    public Worth getWorth(String key) {
        return cache.get(key);
    }
}
