package dev.hyronic.exstorage.listeners.storage;

import com.google.common.base.Strings;
import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.storage.Storage;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.util.ActionBar;
import dev.hyronic.exstorage.util.Digital;
import dev.hyronic.exstorage.util.Utils;
import dev.hyronic.exstorage.util.VersionUtils;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public final class VanillaPickupListener
        extends StorageListener {

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
