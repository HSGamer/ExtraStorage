package me.hsgamer.extrastorage.listeners;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.stack.item.StackedItemManager;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.hscore.bukkit.utils.VersionUtils;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

public class PickupListener implements Listener {
    private final ExtraStorage instance;
    private final PickupHandler pickupHandler;

    public PickupListener(ExtraStorage instance) {
        this.instance = instance;
        this.pickupHandler = getPickupHandler();
        register();
    }

    private PickupHandler getPickupHandler() {
        PluginManager pluginManager = instance.getServer().getPluginManager();
        if (pluginManager.isPluginEnabled("WildStacker"))
            return new PickupHandler() {
                @Override
                public int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item) {
                    return WildStackerAPI.getItemAmount(entity);
                }

                @Override
                public void applyAmount(Item entity, ItemStack item, int amount) {
                    com.bgsoftware.wildstacker.api.objects.StackedItem sItem = WildStackerAPI.getStackedItem(entity);
                    sItem.setStackAmount(amount, true);
                }
            };
        else if (pluginManager.isPluginEnabled("UltimateStacker"))
            return new PickupHandler() {
                @Override
                public int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item) {
                    StackedItemManager manager = UltimateStackerApi.getStackedItemManager();
                    return manager.isStackedItem(entity) ? manager.getActualItemAmount(entity) : item.getAmount();
                }

                @Override
                public void applyAmount(Item entity, ItemStack item, int amount) {
                    StackedItemManager manager = UltimateStackerApi.getStackedItemManager();
                    manager.updateStack(entity, amount);
                }
            };
        else if (pluginManager.isPluginEnabled("RoseStacker"))
            return new PickupHandler() {
                @Override
                public EventPriority getPickupPriority() {
                    return EventPriority.LOWEST;
                }

                @Override
                public int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item) {
                    RoseStackerAPI api = RoseStackerAPI.getInstance();
                    dev.rosewood.rosestacker.stack.StackedItem stackedItem = api.getStackedItem(entity);
                    return stackedItem != null ? stackedItem.getStackSize() : item.getAmount();
                }

                @Override
                public void applyAmount(Item entity, ItemStack item, int amount) {
                    RoseStackerAPI api = RoseStackerAPI.getInstance();
                    dev.rosewood.rosestacker.stack.StackedItem stackedItem = api.getStackedItem(entity);
                    if (stackedItem != null) {
                        stackedItem.setStackSize(amount);
                    } else {
                        item.setAmount(amount);
                        entity.setItemStack(item);
                    }
                }
            };
        else
            return new PickupHandler() {
                @Override
                public int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item) {
                    int amount = item.getAmount();
                    if (VersionUtils.isAtLeast(17)) {
                        amount += event.getRemaining();
                    }
                    return amount;
                }

                @Override
                public void applyAmount(Item entity, ItemStack item, int amount) {
                    item.setAmount(amount);
                    entity.setItemStack(item);
                }
            };
    }

    private void register() {
        instance.getServer().getPluginManager().registerEvent(EntityPickupItemEvent.class, this, pickupHandler.getPickupPriority(), (listener, event) -> {
            if (event instanceof EntityPickupItemEvent) {
                EntityPickupItemEvent pickupEvent = (EntityPickupItemEvent) event;
                onEntityPickupItem(pickupEvent);
            }
        }, instance, true);
    }

    private void onEntityPickupItem(EntityPickupItemEvent event) {
        if ((!instance.getSetting().isPickupToStorage()) || (!(event.getEntity() instanceof Player))) return;
        Player player = (Player) event.getEntity();

        Item entity = event.getItem();
        if (instance.getSetting().getBlacklistWorlds().contains(entity.getWorld().getName())) return;
        ItemStack item = entity.getItemStack().clone();

        User user = instance.getUserManager().getUser(player);
        if (!user.hasPermission(Constants.STORAGE_PICKUP_PERMISSION)) return;

        String validKey = ItemUtil.toMaterialKey(item);
        if (instance.getSetting().getBlacklist().contains(validKey) || (instance.getSetting().isLimitWhitelist() && !instance.getSetting().getWhitelist().contains(validKey))) return;

        Storage storage = user.getStorage();
        if (storage.isMaxSpace() || (!storage.canStore(item))) return;

        int amount = pickupHandler.getAmount(event, entity, item);
        int result = amount;

        long freeSpace = storage.getFreeSpace();
        long maxTake = Math.min(amount, freeSpace == -1 ? Integer.MAX_VALUE : Math.min(freeSpace, Integer.MAX_VALUE));
        amount = (int) maxTake;
        if ((freeSpace != -1) && (freeSpace < amount)) {
            result = (int) freeSpace;
            int residual = amount - result;

            pickupHandler.applyAmount(entity, item, residual);
        } else {
            event.setCancelled(true);
            entity.remove();
        }
        ListenerUtil.addToStorage(player, storage, item, result);
    }

    private interface PickupHandler {
        default EventPriority getPickupPriority() {
            return EventPriority.LOW;
        }

        int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item);

        void applyAmount(Item entity, ItemStack item, int amount);
    }
}
