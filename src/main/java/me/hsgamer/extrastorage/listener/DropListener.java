package me.hsgamer.extrastorage.listener;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.CacheManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;

import java.util.List;

public class DropListener implements Loadable {
    private static final boolean BLOCK_DROP_ITEM_EVENT_AVAILABLE = isBlockDropItemEventAvailable();
    private final ExtraStorage instance;
    private Listener listener;
    public DropListener(ExtraStorage instance) {
        this.instance = instance;
    }

    private static boolean isBlockDropItemEventAvailable() {
        try {
            Class.forName("org.bukkit.event.block.BlockDropItemEvent");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public void enable() {
        if (!BLOCK_DROP_ITEM_EVENT_AVAILABLE || listener != null) return;
        listener = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onBlockDropItem(BlockDropItemEvent event) {
                handleDrop(event.getPlayer(), event.getBlock().getWorld(), event.getItems());
            }
        };
        instance.getServer().getPluginManager().registerEvents(listener, instance);
    }

    @Override
    public void disable() {
        if (listener == null) return;
        HandlerList.unregisterAll(listener);
        listener = null;
    }

    private void handleDrop(Player player, World world, List<Item> items) {
        if (!instance.get(SettingConfig.class).autoStoreItem() || instance.get(CacheManager.class).isBlacklistedWorld(world))
            return;

        User user = instance.get(UserManager.class).getUser(player);
        Storage storage = user.getStorage();
        if (!storage.getStatus()) return;

        if (items.isEmpty()) return;

        storage.consumeStack(items,
                stack -> {
                    String validKey = ItemUtil.toMaterialKey(stack);
                    return !instance.get(CacheManager.class).getBlacklist().contains(validKey)
                            && (!instance.get(SettingConfig.class).limitWhitelist() || instance.get(CacheManager.class).getWhitelist().contains(validKey))
                            && storage.canStore(stack)
                            && ItemUtil.canStore(player, stack);
                },
                ListenerUtil.getStoredNotifier(player),
                -1);
    }
}
