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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class PickupListener implements Listener, ListenerComponent {
    private static final boolean ENTITY_PICKUP_ITEM_EVENT_AVAILABLE = isEntityPickupItemEventAvailable();
    private final ExtraStorage instance;
    private PickupHandler pickupHandler;
    public PickupListener(ExtraStorage instance) {
        this.instance = instance;
    }

    private static boolean isEntityPickupItemEventAvailable() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public JavaPlugin getPlugin() {
        return instance;
    }

    @Override
    public void enable() {
        this.pickupHandler = getPickupHandler();
        EventPriority priority = pickupHandler.getPickupPriority();
        if (ENTITY_PICKUP_ITEM_EVENT_AVAILABLE) {
            EntityPickupEvent.register(instance, this, priority, this::onPickup);
        } else {
            PlayerPickupEvent.register(instance, this, priority, this::onPickup);
        }
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
                public int getAmount(Item entity, ItemStack item, int remaining) {
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
                public int getAmount(Item entity, ItemStack item, int remaining) {
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
                public int getAmount(Item entity, ItemStack item, int remaining) {
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
                public int getAmount(Item entity, ItemStack item, int remaining) {
                    return item.getAmount() + remaining;
                }

                @Override
                public void applyAmount(Item entity, ItemStack item, int amount) {
                    item.setAmount(amount);
                    entity.setItemStack(item);
                }
            };
    }

    private void onPickup(PickupEvent event) {
        if (!instance.get(SettingConfig.class).pickupToStorage()) return;
        Player player = event.getPlayer();
        if (player == null) return;

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

        int amount = pickupHandler.getAmount(entity, item, event.getRemaining());
        storage.consumeStack(item, amount,
                residual -> pickupHandler.applyAmount(entity, item, residual),
                () -> {
                    event.setCancelled(true);
                    entity.remove();
                },
                ListenerUtil.getStoredNotifier(player));
    }

    private interface PickupEvent {
        Player getPlayer();

        Item getItem();

        int getRemaining();

        void setCancelled(boolean cancelled);
    }

    private interface PickupHandler {
        default EventPriority getPickupPriority() {
            return EventPriority.LOW;
        }

        default boolean hasProblem() {
            return false;
        }

        int getAmount(Item entity, ItemStack item, int remaining);

        void applyAmount(Item entity, ItemStack item, int amount);
    }

    private static final class EntityPickupEvent implements PickupEvent {
        private final EntityPickupItemEvent event;

        private EntityPickupEvent(EntityPickupItemEvent event) {
            this.event = event;
        }

        static void register(JavaPlugin plugin, Listener listener, EventPriority priority, Consumer<PickupEvent> handler) {
            plugin.getServer().getPluginManager().registerEvent(EntityPickupItemEvent.class, listener, priority,
                    (l, event) -> handler.accept(new EntityPickupEvent((EntityPickupItemEvent) event)), plugin, true);
        }

        @Override
        public Player getPlayer() {
            return event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        }

        @Override
        public Item getItem() {
            return event.getItem();
        }

        @Override
        public int getRemaining() {
            // getRemaining was added to EntityPickupItemEvent in 1.17
            return VersionUtils.isAtLeast(17) ? event.getRemaining() : 0;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            event.setCancelled(cancelled);
        }
    }

    private static final class PlayerPickupEvent implements PickupEvent {
        private final PlayerPickupItemEvent event;

        private PlayerPickupEvent(PlayerPickupItemEvent event) {
            this.event = event;
        }

        static void register(JavaPlugin plugin, Listener listener, EventPriority priority, Consumer<PickupEvent> handler) {
            plugin.getServer().getPluginManager().registerEvent(PlayerPickupItemEvent.class, listener, priority,
                    (l, event) -> handler.accept(new PlayerPickupEvent((PlayerPickupItemEvent) event)), plugin, true);
        }

        @Override
        public Player getPlayer() {
            return event.getPlayer();
        }

        @Override
        public Item getItem() {
            return event.getItem();
        }

        @Override
        public int getRemaining() {
            return event.getRemaining();
        }

        @Override
        public void setCancelled(boolean cancelled) {
            event.setCancelled(cancelled);
        }
    }
}
