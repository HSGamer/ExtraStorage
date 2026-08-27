package me.hsgamer.extrastorage.listener;

import io.github.projectunified.minelib.plugin.listener.ListenerComponent;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.CacheManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class DropListener implements ListenerComponent {
    private final ExtraStorage instance;

    public DropListener(ExtraStorage instance) {
        this.instance = instance;
    }

    public static boolean isAvailable() {
        try {
            Class.forName("org.bukkit.event.block.BlockDropItemEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!instance.get(SettingConfig.class).autoStoreItem() || instance.get(CacheManager.class).isBlacklistedWorld(event.getBlock().getWorld()))
            return;

        Player player = event.getPlayer();

        User user = instance.get(UserManager.class).getUser(player);
        Storage storage = user.getStorage();
        if (!storage.getStatus()) return;

        List<Item> items = event.getItems();
        if (items.isEmpty()) return;

        storage.consumeStack(items,
                stack -> {
                    String validKey = ItemUtil.toMaterialKey(stack);
                    return !instance.get(CacheManager.class).getBlacklist().contains(validKey)
                            && (!instance.get(SettingConfig.class).limitWhitelist() || instance.get(CacheManager.class).getWhitelist().contains(validKey))
                            && storage.canStore(stack)
                            && ItemUtil.canStore(player, stack);
                },
                ListenerUtil.getStoredNotifier(player));
    }
}
