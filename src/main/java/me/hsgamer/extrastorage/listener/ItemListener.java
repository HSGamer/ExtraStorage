package me.hsgamer.extrastorage.listener;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.projectunified.minelib.plugin.listener.ListenerComponent;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private final Cache<String, User> locCache;

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

    private String locToString(Location loc) {
        return String.format("%s:%d:%d:%d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean canStore(Player player, ItemStack item) {
        if (!instance.get(SettingConfig.class).onlyStoreWhenInvFull()) return true;

        ItemStack[] items = player.getInventory().getStorageContents();
        int count = item.getAmount();
        for (ItemStack iStack : items) {
            if (count < 1) break;
            if ((iStack == null) || (iStack.getType() == Material.AIR)) {
                count -= item.getMaxStackSize();
                continue;
            }
            if (!iStack.isSimilar(item)) continue;
            int stackLeft = item.getMaxStackSize() - iStack.getAmount();
            count -= stackLeft;
        }
        return (count > 0);
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
        String locToString = locToString(location);

        if (instance.get(SettingConfig.class).blacklistWorlds().contains(location.getWorld().getName()) || (!storage.getStatus())) {
            locCache.invalidate(locToString);
            return;
        }

        if (instance.get(SettingConfig.class).blockedMining() && storage.isMaxSpace()) {
            event.setCancelled(true);
            locCache.invalidate(locToString);

            String msg = Utils.formatMessage(instance.get(MessageConfig.class).warn().storageIsFull());
            if (!Strings.isNullOrEmpty(msg)) ActionBar.send(player, msg);
            return;
        }

        User cur = locCache.getIfPresent(locToString);
        if ((cur == null) || (cur.hashCode() != user.hashCode())) locCache.put(locToString, user);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!instance.get(SettingConfig.class).autoStoreItem()) return;

        Location loc = event.getLocation();
        String locToString = this.locToString(loc);

        User user = locCache.getIfPresent(locToString);
        if (user == null || !user.isOnline()) return;

        Storage storage = user.getStorage();

        ItemStack item = event.getEntity().getItemStack();

        String validKey = ItemUtil.toMaterialKey(item);
        if (instance.get(SettingConfig.class).getNormalizedBlacklist().contains(validKey) || (instance.get(SettingConfig.class).limitWhitelist() && !instance.get(SettingConfig.class).getNormalizedWhitelist().contains(validKey)))
            return;

        if (storage.isMaxSpace() || (!this.canStore(user.getPlayer(), item)) || (!storage.canStore(item))) return;

        boolean isResidual = false;
        int amount = item.getAmount();
        long freeSpace = storage.getFreeSpace();
        long maxTake = Math.min(amount, freeSpace == -1 ? Integer.MAX_VALUE : Math.min(freeSpace, Integer.MAX_VALUE));
        amount = (int) maxTake;

        if ((freeSpace != -1) && (freeSpace < amount)) {
            amount = (int) freeSpace;
            item.setAmount(item.getAmount() - amount);
            isResidual = true;
        }

        if (!isResidual) event.setCancelled(true);
        ListenerUtil.addToStorage(user.getPlayer(), storage, item, amount);
    }
}
