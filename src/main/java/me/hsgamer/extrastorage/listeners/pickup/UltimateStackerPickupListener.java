package me.hsgamer.extrastorage.listeners.pickup;

import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.stack.item.StackedItemManager;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.util.ListenerUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public final class UltimateStackerPickupListener
        extends PickupListener {

    public UltimateStackerPickupListener(ExtraStorage instance) {
        super(instance);
    }

    @Override
    public void onPickup(EntityPickupItemEvent event, Player player, Storage storage, Item entity, ItemStack item) {
        StackedItemManager manager = UltimateStackerApi.getStackedItemManager();
        int amount = (manager.isStackedItem(entity) ? manager.getActualItemAmount(entity) : item.getAmount()), result = amount;

        long freeSpace = storage.getFreeSpace();
        if ((freeSpace != -1) && (freeSpace < amount)) {
            result = (int) freeSpace;
            int residual = amount - result;

            manager.updateStack(entity, residual);

            item.setAmount(residual);
            entity.setItemStack(item);
        } else {
            event.setCancelled(true);
            entity.remove();
        }
        ListenerUtil.addToStorage(player, storage, item, result);
    }
}
