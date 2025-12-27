package me.hsgamer.extrastorage.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.projectunified.uniitem.all.AllItemProvider;
import io.github.projectunified.uniitem.api.Item;
import io.github.projectunified.uniitem.api.ItemKey;
import me.hsgamer.extrastorage.Debug;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.Constants;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ItemUtil {
    public static final AllItemProvider provider = new AllItemProvider();
    private static final Map<String, String> keyCache = new ConcurrentHashMap<>();
    private static final LoadingCache<String, Item> itemCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Item>() {
                @Override
                public @NotNull Item load(@NotNull String key) {
                    try {
                        ItemKey itemKey = ItemKey.fromString(key);
                        Item item = provider.wrap(itemKey);
                        if (item.isValid()) {
                            return item;
                        }
                    } catch (Exception e) {
                        // IGNORED
                    }

                    Material material = null;

                    String[] split = key.split(":", 2);
                    if (split.length == 2) {
                        String possibleType = split[0];

                        Material possibleMaterial = Material.matchMaterial(possibleType);
                        if (possibleMaterial != null) {
                            material = possibleMaterial;
                        }
                    }

                    if (material == null) {
                        material = Material.matchMaterial(key);
                    }

                    if (material != null) {
                        return new VanillaItem(material);
                    }

                    return Item.INVALID;
                }
            });

    public static String toMaterialKey(Object key) {
        if (key instanceof ItemStack) {
            ItemStack item = (ItemStack) key;

            Item validItem = getItem(item);
            ItemKey itemKey = validItem.key();

            String keyStr;
            if (itemKey == null) {
                keyStr = Constants.INVALID;
            } else {
                if (validItem instanceof VanillaItem && itemKey.type().equals(NamespacedKey.MINECRAFT)) {
                    keyStr = ((VanillaItem) validItem).getName();
                } else {
                    keyStr = itemKey.type().toUpperCase(Locale.ROOT) + ":" + itemKey.id();
                }
            }

            Debug.log(
                    "[ITEM] Item: " + item,
                    "[ITEM] ItemKey: " + itemKey,
                    "[ITEM] KeyStr: " + keyStr
            );

            return keyStr;
        } else {
            String keyStr = normalizeMaterialKey(key.toString());
            Debug.log(
                    "[KEY] Key: " + key,
                    "[KEY] KeyStr: " + keyStr
            );
            return keyStr;
        }
    }

    /**
     * Normalize the key
     *
     * @param key the key
     * @return the normalized key
     */
    public static String normalizeMaterialKey(String key) {
        return keyCache.computeIfAbsent(key, k -> {
            String[] split = k.split(":", 2);
            if (split.length == 1) {
                return k;
            }

            String possibleType = split[0];
            String possibleId = split[1];

            // Return the type if it's a Material
            Material material = Material.matchMaterial(possibleType);
            if (material != null) {
                return material.name();
            }

            // Always put the type in uppercase
            return possibleType.toUpperCase(Locale.ROOT) + ":" + possibleId;
        });
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

    public static Item getItem(String key) {
        try {
            return itemCache.get(key);
        } catch (ExecutionException e) {
            ExtraStorage.getInstance().getLogger().log(Level.SEVERE, "[ITEM] Error getting item: " + key, e);
            return Item.INVALID;
        }
    }

    public static Item getItem(ItemStack item) {
        return item.hasItemMeta() ? provider.wrap(item) : new VanillaItem(item.getType());
    }

    public static ItemType getItemType(Item item) {
        if (!item.isValid()) {
            return ItemType.NONE;
        }
        return item instanceof VanillaItem ? ItemType.VANILLA : ItemType.CUSTOM;
    }

    public enum ItemType {
        NONE, VANILLA, CUSTOM
    }

    public static final class VanillaItem implements Item {
        private final @Nullable Material material;

        public VanillaItem(@Nullable Material material) {
            this.material = material;
        }

        public String getName() {
            return material != null ? material.name() : null;
        }

        @Override
        public boolean isValid() {
            return material != null;
        }

        @Override
        public @Nullable ItemKey key() {
            if (material == null) return null;
            NamespacedKey key = material.getKey();
            return new ItemKey(key.getNamespace(), key.getKey());
        }

        @Override
        public @Nullable ItemStack bukkitItem() {
            if (material == null) return null;
            return new ItemStack(material);
        }

        @Override
        public @Nullable ItemStack bukkitItem(@NotNull Player player) {
            return bukkitItem();
        }

        @Override
        public boolean isSimilar(@NotNull ItemStack itemStack) {
            if (material == null) return false;
            return !itemStack.hasItemMeta() && Objects.equals(material, itemStack.getType());
        }
    }
}
