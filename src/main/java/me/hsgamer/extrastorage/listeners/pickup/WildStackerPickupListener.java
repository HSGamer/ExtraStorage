package me.hsgamer.extrastorage.listeners.pickup;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.util.ListenerUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public final class WildStackerPickupListener
        extends PickupListener {

    public WildStackerPickupListener(ExtraStorage instance) {
        super(instance);
    }

    @Override
    public void onPickup(EntityPickupItemEvent event, Player player, Storage storage, Item entity, ItemStack item) {
        int amount = WildStackerAPI.getItemAmount(entity), result = amount;

        long freeSpace = storage.getFreeSpace();
        if ((freeSpace != -1) && (freeSpace < amount)) {
            result = (int) freeSpace;
            int residual = amount - result;

            StackedItem sItem = WildStackerAPI.getStackedItem(entity);
            sItem.setStackAmount(residual, true);

            item.setAmount(residual);
            entity.setItemStack(item);
        } else {
            event.setCancelled(true);
            entity.remove();
        }
        ListenerUtil.addToStorage(player, storage, item, result);
    }

}
