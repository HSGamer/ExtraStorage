package me.hsgamer.extrastorage.listeners;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
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

import java.util.concurrent.TimeUnit;

public class ItemListener extends BaseListener {
    private final Cache<String, User> locCache;
    private final UserManager manager;

    public ItemListener(ExtraStorage instance) {
        super(instance);
        this.manager = instance.getUserManager();
        this.locCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build();
    }

    private String locToString(Location loc) {
        return String.format("%s:%d:%d:%d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean canStore(Player player, ItemStack item) {
        if (!instance.getSetting().isOnlyStoreWhenInvFull()) return true;

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

        User user = manager.getUser(player);
        Storage storage = user.getStorage();
        Location location = event.getBlock().getLocation();
        String locToString = locToString(location);

        if (instance.getSetting().getBlacklistWorlds().contains(location.getWorld().getName()) || (!storage.getStatus())) {
            locCache.invalidate(locToString);
            return;
        }

        if (instance.getSetting().isBlockedMining() && storage.isMaxSpace()) {
            event.setCancelled(true);
            locCache.invalidate(locToString);

            String msg = Message.getMessage("WARN.StorageIsFull");
            if (!Strings.isNullOrEmpty(msg)) ActionBar.send(player, msg);
            return;
        }

        User cur = locCache.getIfPresent(locToString);
        if ((cur == null) || (cur.hashCode() != user.hashCode())) locCache.put(locToString, user);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!instance.getSetting().isAutoStoreItem()) return;

        Location loc = event.getLocation();
        String locToString = this.locToString(loc);

        User user = locCache.getIfPresent(locToString);
        if (user == null || !user.isOnline()) return;

        Storage storage = user.getStorage();

        ItemStack item = event.getEntity().getItemStack();
        if (storage.isMaxSpace() || (!this.canStore(user.getPlayer(), item)) || (!storage.canStore(item))) return;

        boolean isResidual = false;
        int amount = item.getAmount();
        long freeSpace = storage.getFreeSpace();

        // Giới hạn số lượng lấy ra tối đa là Integer.MAX_VALUE
        long maxTake = Math.min(amount, freeSpace == -1 ? Integer.MAX_VALUE : Math.min(freeSpace, Integer.MAX_VALUE));
        amount = (int) maxTake;

        if (!isResidual) event.setCancelled(true);
        storage.add(item, amount);

        if (!Strings.isNullOrEmpty(Message.getMessage("WARN.Stored.ActionBar"))) {
            ActionBar.send(user.getPlayer(), Message.getMessage("WARN.Stored.ActionBar")
                    .replaceAll(Utils.getRegex("current"), Digital.formatThousands(storage.getItem(item).get().getQuantity() > Integer.MAX_VALUE ? Integer.MAX_VALUE : storage.getItem(item).get().getQuantity()))
                    .replaceAll(Utils.getRegex("quantity", "amount"), String.valueOf(amount))
                    .replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(item, true)));
        }
    }
}
