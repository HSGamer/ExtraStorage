package me.hsgamer.extrastorage.listeners.pickup;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.util.ListenerUtil;
import me.hsgamer.extrastorage.util.VersionUtils;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public final class VanillaPickupListener
        extends PickupListener {

    public VanillaPickupListener(ExtraStorage instance) {
        super(instance);
    }

    @Override
    public void onPickup(EntityPickupItemEvent event, Player player, Storage storage, Item entity, ItemStack item) {
        int amount = item.getAmount();

        if (VersionUtils.isAtLeast(17)) {
            /*
             * Từ phiên bản 1.17 trở lên, khi nhặt vật phẩm với số lượng còn dư thừa,
             * chúng sẽ bị mất đi phần bị dư và số lượng được đưa vào kho không đúng.
             */
            amount += event.getRemaining();
        }

        boolean isResidual = false;
        long freeSpace = storage.getFreeSpace();
        // Giới hạn số lượng lấy ra tối đa là Integer.MAX_VALUE
        long maxTake = Math.min(amount, freeSpace == -1 ? Integer.MAX_VALUE : Math.min(freeSpace, Integer.MAX_VALUE));
        amount = (int) maxTake;
        if ((freeSpace != -1) && (freeSpace < amount)) {
            amount = (int) freeSpace;
            int residual = item.getAmount() - amount;
            item.setAmount(residual);
            isResidual = true;
        }

        if (!isResidual) {
            event.setCancelled(true);
            entity.remove();
        } else entity.setItemStack(item);
        ListenerUtil.addToStorage(player, storage, item, amount);
    }

}
