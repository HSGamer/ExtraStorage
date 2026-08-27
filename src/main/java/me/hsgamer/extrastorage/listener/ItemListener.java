package me.hsgamer.extrastorage.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.projectunified.minelib.plugin.listener.ListenerComponent;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.CacheManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class ItemListener implements ListenerComponent {
    private final ExtraStorage instance;
    private final Cache<Location, User> locCache;

    public ItemListener(ExtraStorage instance) {
        this.instance = instance;
        this.locCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return;
        }

        User user = instance.get(UserManager.class).getUser(player);
        Storage storage = user.getStorage();
        Location location = event.getBlock().getLocation();

        if (instance.get(CacheManager.class).isBlacklistedWorld(location.getWorld()) || (!storage.getStatus()) || storage.isMaxSpace()) {
            locCache.invalidate(location);
            return;
        }

        User cur = locCache.getIfPresent(location);
        if ((cur == null) || (cur.hashCode() != user.hashCode())) locCache.put(location, user);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!instance.get(SettingConfig.class).autoStoreItem()) return;

        Location loc = event.getLocation();
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        User user = locCache.getIfPresent(blockLoc);
        if (user == null || !user.isOnline()) return;

        Storage storage = user.getStorage();

        ItemStack item = event.getEntity().getItemStack();

        String validKey = ItemUtil.toMaterialKey(item);
        if (instance.get(CacheManager.class).getBlacklist().contains(validKey) || (instance.get(SettingConfig.class).limitWhitelist() && !instance.get(CacheManager.class).getWhitelist().contains(validKey)))
            return;

        int amount = item.getAmount();
        if (!storage.canStore(item) || !ItemUtil.canStore(user.getPlayer(), item)) return;
        storage.consumeStack(item, amount,
                item::setAmount,
                () -> event.setCancelled(true),
                ListenerUtil.getStoredNotifier(user.getPlayer()));
    }
}
