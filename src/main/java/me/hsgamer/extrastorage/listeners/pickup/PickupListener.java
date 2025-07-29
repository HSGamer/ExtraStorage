package me.hsgamer.extrastorage.listeners.pickup;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.listeners.BaseListener;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public abstract class PickupListener extends BaseListener {
    protected final UserManager manager;

    public PickupListener(ExtraStorage instance) {
        super(instance);
        this.manager = instance.getUserManager();
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

    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if ((!instance.getSetting().isPickupToStorage()) || (!(event.getEntity() instanceof Player))) return;
        Player player = (Player) event.getEntity();

        Item entity = event.getItem();
        if (instance.getSetting().getBlacklistWorlds().contains(entity.getWorld().getName())) return;
        ItemStack item = entity.getItemStack().clone();

        User user = instance.getUserManager().getUser(player);
        if (!user.hasPermission(Constants.STORAGE_PICKUP_PERMISSION)) return;

        Storage storage = user.getStorage();
        if (storage.isMaxSpace() || (!storage.canStore(item))) return;

        this.onPickup(event, player, storage, entity, item);
    }

    public EventPriority getPickupPriority() {
        return EventPriority.LOW;
    }

    public abstract void onPickup(EntityPickupItemEvent event, Player player, Storage storage, Item entity, ItemStack item);
}
