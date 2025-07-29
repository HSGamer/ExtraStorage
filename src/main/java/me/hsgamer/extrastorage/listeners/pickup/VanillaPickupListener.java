package me.hsgamer.extrastorage.listeners.pickup;

import com.google.common.base.Strings;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
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
        storage.add(item, amount);

        if (instance.getSetting().getPickupSound() != null)
            player.playSound(player.getLocation(), instance.getSetting().getPickupSound(), 4.0f, 2.0f);

        if (!Strings.isNullOrEmpty(Message.getMessage("WARN.Stored.ActionBar"))) {
            ActionBar.send(player, Message.getMessage("WARN.Stored.ActionBar")
                    .replaceAll(Utils.getRegex("current"), Digital.formatThousands(storage.getItem(item).get().getQuantity()))
                    .replaceAll(Utils.getRegex("quantity", "amount"), String.valueOf(amount))
                    .replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(item, true)));
        }
    }

}
