package me.hsgamer.extrastorage.gui.util;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.configs.Setting;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;

public final class SortUtil {
    private SortUtil() {
    }

    public static int compareItemByMaterial(Item obj1, Item obj2) {
        return obj1.getKey().compareTo(obj2.getKey());
    }

    public static int compareItemByName(Item obj1, Item obj2) {
        Setting setting = ExtraStorage.getInstance().getSetting();
        String name1 = setting.getNameFormatted(obj1.getKey(), false);
        String name2 = setting.getNameFormatted(obj2.getKey(), false);
        return name1.compareTo(name2);
    }

    public static int compareItemByQuantity(Item obj1, Item obj2) {
        return Long.compare(obj2.getQuantity(), obj1.getQuantity());
    }

    public static int comparePartnerByName(Partner obj1, Partner obj2) {
        OfflinePlayer p1 = obj1.getOfflinePlayer(), p2 = obj2.getOfflinePlayer();
        return p1.getName().compareTo(p2.getName());
    }

    public static int comparePartnerByTimestamp(Partner obj1, Partner obj2) {
        return Long.compare(obj2.getTimestamp(), obj1.getTimestamp());
    }

    @SafeVarargs
    public static <T> Comparator<T> compose(boolean orderSort, Comparator<T>... comparator) {
        Comparator<T> allCompare = (obj1, obj2) -> {
            int compare = 0;
            for (Comparator<T> comparator1 : comparator) {
                compare = comparator1.compare(obj1, obj2);
                if (compare != 0) {
                    break;
                }
            }
            return compare;
        };
        if (orderSort) {
            return allCompare;
        } else {
            return allCompare.reversed();
        }
    }
}
