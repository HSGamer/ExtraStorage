package me.hsgamer.extrastorage.gui.util;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.configs.Setting;
import org.bukkit.OfflinePlayer;

public final class SortUtil {
    private SortUtil() {
    }

    public static int compareItemByMaterial(Item obj1, Item obj2, boolean orderSort) {
        int compare = orderSort ? obj1.getKey().compareTo(obj2.getKey()) : obj2.getKey().compareTo(obj1.getKey());
        if (compare == 0) {
            compare = orderSort
                    ? Integer.compare((int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE))
                    : Integer.compare((int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE));
        }
        return compare;
    }

    public static int compareItemByName(Item obj1, Item obj2, boolean orderSort) {
        Setting setting = ExtraStorage.getInstance().getSetting();
        String name1 = setting.getNameFormatted(obj1.getKey(), false);
        String name2 = setting.getNameFormatted(obj2.getKey(), false);
        int compare = orderSort ? name1.compareTo(name2) : name2.compareTo(name1);
        if (compare == 0) {
            compare = orderSort
                    ? Integer.compare((int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE))
                    : Integer.compare((int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE));
        }
        return compare;
    }

    public static int compareItemByQuantity(Item obj1, Item obj2, boolean orderSort) {
        int compare = orderSort ? Long.compare(obj2.getQuantity(), obj1.getQuantity()) : Long.compare(obj1.getQuantity(), obj2.getQuantity());
        if (compare == 0) {
            compare = orderSort ? obj1.getKey().compareTo(obj2.getKey()) : obj2.getKey().compareTo(obj1.getKey());
        }
        return compare;
    }

    public static int comparePartnerByName(Partner obj1, Partner obj2, boolean orderSort) {
        OfflinePlayer p1 = obj1.getOfflinePlayer(), p2 = obj2.getOfflinePlayer();
        int compare = orderSort ? p1.getName().compareTo(p2.getName()) : p2.getName().compareTo(p1.getName());
        if (compare == 0) {
            compare = orderSort ? Long.compare(obj2.getTimestamp(), obj1.getTimestamp()) : Long.compare(obj1.getTimestamp(), obj2.getTimestamp());
        }
        return compare;
    }

    public static int comparePartnerByTimestamp(Partner obj1, Partner obj2, boolean orderSort) {
        int compare = orderSort ? Long.compare(obj2.getTimestamp(), obj1.getTimestamp()) : Long.compare(obj1.getTimestamp(), obj2.getTimestamp());
        if (compare == 0) {
            OfflinePlayer p1 = obj1.getOfflinePlayer(), p2 = obj2.getOfflinePlayer();
            compare = orderSort ? p1.getName().compareTo(p2.getName()) : p2.getName().compareTo(p1.getName());
        }
        return compare;
    }
}
