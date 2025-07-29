package me.hsgamer.extrastorage.listeners.pickup;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedItem;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.util.ListenerUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class RoseStackerPickupListener extends PickupListener {
    public RoseStackerPickupListener(ExtraStorage instance) {
        super(instance);
    }

    @Override
    public EventPriority getPickupPriority() {
        return EventPriority.LOWEST;
    }

    @Override
    public void onPickup(EntityPickupItemEvent event, Player player, Storage storage, Item entity, ItemStack item) {
        RoseStackerAPI api = RoseStackerAPI.getInstance();
        StackedItem stackedItem = api.getStackedItem(entity);
        int amount = (stackedItem != null ? stackedItem.getStackSize() : item.getAmount()), result = amount;

        long freeSpace = storage.getFreeSpace();
        if ((freeSpace != -1) && (freeSpace < amount)) {
            result = (int) freeSpace;
            int residual = amount - result;

            if (stackedItem != null) {
                stackedItem.setStackSize(residual);
            }

            item.setAmount(residual);
            entity.setItemStack(item);
        } else {
            event.setCancelled(true);
            entity.remove();
        }
        ListenerUtil.addToStorage(player, storage, item, result);
    }
}
