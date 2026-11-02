package me.hsgamer.extrastorage.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.CacheManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DropListener implements Loadable {
    private final ExtraStorage instance;
    private final List<Listener> listeners = new ArrayList<>();
    private final Cache<Location, User> locCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build();

    public DropListener(ExtraStorage instance) {
        this.instance = instance;
    }

    private static boolean isBlockDropEventAvailable() {
        try {
            Class.forName("org.bukkit.event.block.BlockDropItemEvent");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public void enable() {
        listeners.add(blockBreakListener());
        if (isBlockDropEventAvailable()) {
            listeners.add(blockDropListener());
        }
        listeners.add(itemDropListener());
        listeners.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, instance));
    }

    @Override
    public void disable() {
        listeners.forEach(HandlerList::unregisterAll);
        listeners.clear();
    }

    private Listener blockBreakListener() {
        return new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onBreak(BlockBreakEvent event) {
                if (!instance.get(SettingConfig.class).autoStoreItem()) return;

                Player player = event.getPlayer();
                GameMode gameMode = player.getGameMode();
                if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) return;
                if (instance.get(CacheManager.class).isBlacklistedWorld(event.getBlock().getWorld())) return;

                locCache.put(event.getBlock().getLocation(), instance.get(UserManager.class).getUser(player));
            }
        };
    }

    private Listener blockDropListener() {
        return new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onBlockDropItem(BlockDropItemEvent event) {
                if (!instance.get(SettingConfig.class).autoStoreItem()) return;

                User user = locCache.getIfPresent(event.getBlockState().getLocation());
                if (user == null) return;
                Player player = user.getPlayer();
                if (player == null || !player.isOnline()) return;

                List<Item> items = event.getItems();
                if (items.isEmpty()) return;

                CacheManager cacheManager = instance.get(CacheManager.class);
                Set<String> blacklist = cacheManager.getBlacklist();
                Set<String> whitelist = cacheManager.getWhitelist();
                boolean limitWhitelist = instance.get(SettingConfig.class).limitWhitelist();
                Storage storage = user.getStorage();

                storage.consumeStack(items,
                        stack -> {
                            String validKey = ItemUtil.toMaterialKey(stack);
                            return !blacklist.contains(validKey)
                                    && (!limitWhitelist || whitelist.contains(validKey))
                                    && storage.canStore(stack)
                                    && ItemUtil.canStore(player, stack);
                        },
                        ListenerUtil.getStoredNotifier(player),
                        -1);
            }
        };
    }

    private Listener itemDropListener() {
        return new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onItemSpawn(ItemSpawnEvent event) {
                if (!instance.get(SettingConfig.class).autoStoreItem()) return;

                Location loc = event.getLocation();
                Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                User user = locCache.getIfPresent(blockLoc);
                if (user == null) return;
                Player player = user.getPlayer();
                if (player == null || !player.isOnline()) return;

                ItemStack item = event.getEntity().getItemStack();
                String validKey = ItemUtil.toMaterialKey(item);

                CacheManager cacheManager = instance.get(CacheManager.class);
                if (cacheManager.getBlacklist().contains(validKey) || (instance.get(SettingConfig.class).limitWhitelist() && !cacheManager.getWhitelist().contains(validKey)))
                    return;

                Storage storage = user.getStorage();
                int amount = item.getAmount();
                if (!storage.canStore(validKey) || !ItemUtil.canStore(player, item)) return;
                storage.consumeStack(item, amount,
                        item::setAmount,
                        () -> event.setCancelled(true),
                        ListenerUtil.getStoredNotifier(player));
            }
        };
    }
}
