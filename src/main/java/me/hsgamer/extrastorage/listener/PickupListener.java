package me.hsgamer.extrastorage.listener;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.stack.item.StackedItemManager;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import io.github.projectunified.minelib.plugin.listener.ListenerComponent;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.manager.CacheManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.hscore.bukkit.utils.VersionUtils;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PickupListener implements Listener, ListenerComponent {
    private final ExtraStorage instance;
    private PickupHandler pickupHandler;

    public PickupListener(ExtraStorage instance) {
        this.instance = instance;
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
    }

    @Override
    public void enable() {
        this.pickupHandler = getPickupHandler();
        instance.getServer().getPluginManager().registerEvent(EntityPickupItemEvent.class, this, pickupHandler.getPickupPriority(), (listener, event) -> {
            if (event instanceof EntityPickupItemEvent) {
                EntityPickupItemEvent pickupEvent = (EntityPickupItemEvent) event;
                onEntityPickupItem(pickupEvent);
            }
        }, instance, true);
    }

    private PickupHandler getPickupHandler() {
        PluginManager pluginManager = instance.getServer().getPluginManager();
        if (pluginManager.isPluginEnabled("WildStacker"))
            return new PickupHandler() {
                @Override
                public boolean hasProblem() {
                    return true;
                }

                @Override
                public EventPriority getPickupPriority() {
                    return EventPriority.LOWEST;
                }

                @Override
                public int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item) {
                    return WildStackerAPI.getItemAmount(entity);
                }

                @Override
                public void applyAmount(Item entity, ItemStack item, int amount) {
                    StackedItem sItem = WildStackerAPI.getStackedItem(entity);
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


    private void onEntityPickupItem(EntityPickupItemEvent event) {
        if ((!instance.get(SettingConfig.class).pickupToStorage()) || (!(event.getEntity() instanceof Player))) return;
        Player player = (Player) event.getEntity();

        if (pickupHandler.hasProblem()) return;

        Item entity = event.getItem();
        if (instance.get(CacheManager.class).isBlacklistedWorld(entity.getWorld())) return;
        ItemStack item = entity.getItemStack().clone();

        User user = instance.get(UserManager.class).getUser(player);
        if (!user.hasPermission(Constants.STORAGE_PICKUP_PERMISSION)) return;

        String validKey = ItemUtil.toMaterialKey(item);
        if (instance.get(CacheManager.class).getBlacklist().contains(validKey) || (instance.get(SettingConfig.class).limitWhitelist() && !instance.get(CacheManager.class).getWhitelist().contains(validKey)))
            return;

        Storage storage = user.getStorage();
        if (!storage.canStore(item)) return;

        int amount = pickupHandler.getAmount(event, entity, item);
        storage.consumeStack(item, amount,
                residual -> pickupHandler.applyAmount(entity, item, residual),
                () -> {
                    event.setCancelled(true);
                    entity.remove();
                },
                ListenerUtil.getStoredNotifier(player));
    }

    private interface PickupHandler {
        default EventPriority getPickupPriority() {
            return EventPriority.LOW;
        }

        default boolean hasProblem() {
            return false;
        }

        int getAmount(EntityPickupItemEvent event, Item entity, ItemStack item);

        void applyAmount(Item entity, ItemStack item, int amount);
    }
}
