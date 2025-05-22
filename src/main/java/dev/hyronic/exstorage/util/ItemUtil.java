package dev.hyronic.exstorage.util;

import dev.hyronic.exstorage.Debug;
import dev.hyronic.exstorage.ExtraStorage;
import io.github.projectunified.uniitem.all.AllItemProvider;
import io.github.projectunified.uniitem.api.ItemKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Objects;

import static dev.hyronic.exstorage.data.Constants.INVALID;

public class ItemUtil {
    public static final ExtraStorage instance = ExtraStorage.getInstance();
    public static final AllItemProvider provider = new AllItemProvider();

    /**
     * Validate the key to the material-key
     *
     * @param key the item key. Can be an ItemStack or a string as MATERIAL:DATA
     * @return the material-key
     */
    public static String toMaterialKey(Object key) {
        if (key instanceof ItemStack) {
            ItemStack item = (ItemStack) key;
            String keyStr = INVALID;

            ItemKey itemKey = provider.key(item);
            Debug.log("[KEY] ItemKey: " + itemKey);
            if (itemKey != null) {
                keyStr = itemKey.type().toUpperCase(Locale.ROOT) + ":" + itemKey.id();
            }

            if (Objects.equals(INVALID, keyStr) && !item.hasItemMeta()) {
                keyStr = item.getType().name();
            }

            Debug.log(
                    "[ITEM] Item: " + item,
                    "[ITEM] KeyStr: " + keyStr
            );

            return keyStr;
        } else {
            String keyStr = normalizeMaterialKey(key.toString());
            Debug.log(
                    "[ITEM] Key: " + key,
                    "[ITEM] KeyStr: " + keyStr
            );
            return keyStr;
        }
    }

    private static boolean match(String key, String... keys) {
        for (String k : keys) {
            if (key.equalsIgnoreCase(k)) return true;
        }
        return false;
    }

    // Temporary method to normalize material key
    public static String normalizeMaterialKey(String key) {
        String[] split = key.split(":", 2);
        if (split.length == 1) {
            return key;
        }

        String type = split[0];
        String rest = split[1];

        if (match(type, "oraxen", "orx")) {
            return "ORAXEN:" + rest;
        }
        if (match(type, "itemsadder", "ia")) {
            return "ITEMSADDER:" + rest;
        }
        if (match(type, "nexo")) {
            return "NEXO:" + rest;
        }

        String finalType = provider.getType(type);
        finalType = finalType == null ? type : finalType;
        if (provider.isValidKey(new ItemKey(finalType, rest))) {
            return finalType.toUpperCase(Locale.ROOT) + ":" + rest;
        }

        return type;
    }

    public static ItemPair getItem(String key) {
        String[] split = key.split(":", 2);
        ItemType itemType = ItemType.VANILLA;
        ItemStack item = null;
        if (split.length >= 2) {
            String type = split[0].toLowerCase(Locale.ROOT);
            String id = split[1];

            itemType = ItemType.CUSTOM;
            ItemKey itemKey = new ItemKey(type, id);
            item = provider.item(itemKey);
        }

        if (itemType == ItemType.VANILLA) {
            Material material = Material.matchMaterial(key);
            if (material != null) {
                item = new ItemStack(material, 1);
                item.setItemMeta(null);
            }
        }
        if (item == null) {
            itemType = ItemType.NONE;
        }
        return new ItemPair(item, itemType);
    }

    public static void giveItem(Player player, ItemStack item) {
        int amount = item.getAmount();
        if (amount <= 0) return;

        while (amount > 0) {
            int give = Math.min(amount, item.getMaxStackSize());
            amount -= give;

            ItemStack clone = item.clone();
            clone.setAmount(give);
            player.getInventory().addItem(clone);
        }
    }

    public enum ItemType {
        NONE, VANILLA, CUSTOM
    }

    public static final class ItemPair {
        private final ItemStack item;
        private final ItemType type;

        public ItemPair(ItemStack item, ItemType type) {
            this.item = item;
            this.type = type;
        }

        public ItemStack item() {
            return item;
        }

        public ItemType type() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ItemPair that = (ItemPair) obj;
            return Objects.equals(this.item, that.item) &&
                    Objects.equals(this.type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, type);
        }

        @Override
        public String toString() {
            return "ItemPair[" +
                    "item=" + item + ", " +
                    "type=" + type + ']';
        }

        }
}
