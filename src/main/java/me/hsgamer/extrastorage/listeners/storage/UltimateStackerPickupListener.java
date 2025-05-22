package me.hsgamer.extrastorage.listeners.storage;

import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.stack.item.StackedItemManager;
import com.google.common.base.Strings;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public final class UltimateStackerPickupListener
        extends StorageListener {

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
        storage.add(item, result);

        if (instance.getSetting().getPickupSound() != null)
            player.playSound(player.getLocation(), instance.getSetting().getPickupSound(), 4.0f, 2.0f);

        if (!Strings.isNullOrEmpty(Message.getMessage("WARN.Stored.ActionBar"))) {
            ActionBar.send(player, Message.getMessage("WARN.Stored.ActionBar")
                    .replaceAll(Utils.getRegex("current"), Digital.formatThousands(storage.getItem(item).get().getQuantity()))
                    .replaceAll(Utils.getRegex("quantity", "amount"), String.valueOf(result))
                    .replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(item, true)));
        }
    }

}
