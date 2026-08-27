package me.hsgamer.extrastorage.listener;

import com.google.common.base.Strings;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.CacheManager;
import me.hsgamer.extrastorage.util.ActionBar;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

interface ListenerUtil {
    static BiConsumer<Item, Integer> getStoredNotifier(Player player) {
        return (added, amount) -> {
            SettingConfig setting = ExtraStorage.getInstance().get(SettingConfig.class);
            ExtraStorage.getInstance().get(CacheManager.class).getPickupSoundPlayer().accept(player);

            String actionBarMsg = Utils.formatMessage(ExtraStorage.getInstance().get(MessageConfig.class).warn().stored().actionBar());
            if (!Strings.isNullOrEmpty(actionBarMsg)) {
                ActionBar.send(player, actionBarMsg
                        .replaceAll(Utils.getRegex("current"), Digital.formatThousands(added.getQuantity()))
                        .replaceAll(Utils.getRegex("quantity", "amount"), String.valueOf(amount))
                        .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(added.getKey(), true)));
            }
        };
    }
}
