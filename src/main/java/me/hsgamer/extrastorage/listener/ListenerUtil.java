package me.hsgamer.extrastorage.listener;

import com.google.common.base.Strings;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.HookManager;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

interface ListenerUtil {
    static void addToStorage(Player player, Storage storage, ItemStack item, int amount) {
        storage.add(item, amount);

        SettingConfig setting = ExtraStorage.getInstance().get(SettingConfig.class);
        ExtraStorage.getInstance().get(HookManager.class).getPickupSoundPlayer().accept(player);

        String actionBarMsg = Utils.formatMessage(ExtraStorage.getInstance().get(MessageConfig.class).warn().stored().actionBar());
        if (!Strings.isNullOrEmpty(actionBarMsg)) {
            ActionBar.send(player, actionBarMsg
                    .replaceAll(Utils.getRegex("current"), Digital.formatThousands(storage.getItem(item).get().getQuantity()))
                    .replaceAll(Utils.getRegex("quantity", "amount"), String.valueOf(amount))
                    .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(item, true)));
        }
    }
}
