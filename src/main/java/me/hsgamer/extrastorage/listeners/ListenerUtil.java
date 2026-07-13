package me.hsgamer.extrastorage.listeners;

import com.google.common.base.Strings;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.configs.SettingConfig;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

interface ListenerUtil {
    static void addToStorage(Player player, Storage storage, ItemStack item, int amount) {
        storage.add(item, amount);

        SettingConfig setting = ExtraStorage.getInstance().getSetting();
        setting.getPickupSoundPlayer().accept(player);

        if (!Strings.isNullOrEmpty(ExtraStorage.getInstance().getMessage().getMessage("WARN.Stored.ActionBar"))) {
            ActionBar.send(player, ExtraStorage.getInstance().getMessage().getMessage("WARN.Stored.ActionBar")
                    .replaceAll(Utils.getRegex("current"), Digital.formatThousands(storage.getItem(item).get().getQuantity()))
                    .replaceAll(Utils.getRegex("quantity", "amount"), String.valueOf(amount))
                    .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(item, true)));
        }
    }
}
