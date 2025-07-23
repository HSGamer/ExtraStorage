package me.hsgamer.extrastorage.listeners.storage;

import com.google.common.base.Strings;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.listeners.BaseListener;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StorageListener
        extends BaseListener {

    public final Map<String, User> locCache;
    protected final UserManager manager;

    public StorageListener(ExtraStorage instance) {
        super(instance);
        this.manager = instance.getUserManager();
        this.locCache = new ConcurrentHashMap<>();

        // Để tránh trường hợp bị tràn RAM vì dữ liệu quá nhiều, cần phải dọn dẹp dữ liệu rác thường xuyên.
        new BukkitRunnable() {
            @Override
            public void run() {
                if (locCache.isEmpty()) return;
                synchronized (locCache) {
                    locCache.clear();
                }
            }
        }.runTaskTimerAsynchronously(instance, 20L * 60 * 60 * 3, 20L * 60 * 60 * 3); // 3 phút dọn rác 1 lần.
    }

    @Override
    protected void register() {
        super.register();
        instance.getServer().getPluginManager().registerEvent(EntityPickupItemEvent.class, this, getPickupPriority(), (listener, event) -> {
            if (event instanceof EntityPickupItemEvent) {
                EntityPickupItemEvent pickupEvent = (EntityPickupItemEvent) event;
                onEntityPickupItem(pickupEvent);
            }
        }, instance, true);
    }

    protected final String locToString(Location loc) {
        return (loc.getWorld().getName() + ':' + loc.getBlockX() + ':' + loc.getBlockY() + ':' + loc.getBlockZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    /*
     * Nói sơ qua về phần EventPriority và ignoreCancelled:
     *  + Plugin có EventPriority càng thấp sẽ được gọi trước tiên, ngược lại sẽ được gọi sau cùng.
     *  + Nếu có 1 plugin khác đã cancel event và EventPriority.NORMAL hoặc cao hơn,
     *    nếu thêm ignoreCancelled=true vào, event này sẽ không được gọi. Trường hợp
     *    EventPriority.LOW hoặc LOWEST thì event vẫn được gọi như thường.
     *
     * Sử dụng Event này để xác định player nào đã đào block tại vị trí đó.
     */
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
            locCache.remove(locToString);
            return;
        }

        if (instance.getSetting().isBlockedMining() && storage.isMaxSpace()) {
            event.setCancelled(true);
            locCache.remove(locToString);

            String msg = Message.getMessage("WARN.StorageIsFull");
            if (!Strings.isNullOrEmpty(msg)) ActionBar.send(player, msg);
            return;
        }

        User cur = locCache.get(locToString);
        if ((cur == null) || (cur.hashCode() != user.hashCode())) locCache.put(locToString, user);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!instance.getSetting().isAutoStoreItem()) return;

        Location loc = event.getLocation();
        String locToString = this.locToString(loc);

        User user = locCache.get(locToString);
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

    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if ((!instance.getSetting().isPickupToStorage()) || (!(event.getEntity() instanceof Player))) return;
        Player player = (Player) event.getEntity();

        Item entity = event.getItem();
        if (instance.getSetting().getBlacklistWorlds().contains(entity.getWorld().getName())) return;
        ItemStack item = entity.getItemStack().clone();

        User user = instance.getUserManager().getUser(player);
        if (user == null) return;
        if (!user.hasPermission(Constants.STORAGE_PICKUP_PERMISSION)) return;

        Storage storage = user.getStorage();
        if (storage.isMaxSpace() || (!storage.canStore(item))) return;

        this.onPickup(event, player, storage, entity, item);
    }

    public EventPriority getPickupPriority() {
        return EventPriority.LOW;
    }

    public abstract void onPickup(EntityPickupItemEvent event, Player player, Storage storage, Item entity, ItemStack item);

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

}
