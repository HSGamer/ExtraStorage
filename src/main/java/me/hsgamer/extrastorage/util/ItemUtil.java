package me.hsgamer.extrastorage.util;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.projectunified.uniitem.all.AllItemProvider;
import io.github.projectunified.uniitem.api.Item;
import io.github.projectunified.uniitem.api.ItemKey;
import me.hsgamer.extrastorage.Debug;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.config.SettingConfig;
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
import java.util.Optional;
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

                    Optional<XMaterial> optionalXMaterial = XMaterial.matchXMaterial(key);
                    if (optionalXMaterial.isPresent()) {
                        return new XVanillaItem(optionalXMaterial.get());
                    }

                    String[] split = key.split(":", 2);
                    if (split.length == 2) {
                        String possibleType = split[0];

                        Material material = Material.matchMaterial(possibleType);
                        if (material != null) {
                            try {
                                short durability = Short.parseShort(split[1]);
                                return new VanillaItem(material, durability);
                            } catch (NumberFormatException e) {
                                return new VanillaItem(material, null);
                            }
                        }
                    } else {
                        Material material = Material.matchMaterial(key);
                        if (material != null) {
                            return new VanillaItem(material, null);
                        }
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
                if (validItem instanceof XVanillaItem) {
                    keyStr = ((XVanillaItem) validItem).getName();
                } else if (validItem instanceof VanillaItem && itemKey.type().equals(NamespacedKey.MINECRAFT)) {
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

            Optional<XMaterial> optionalXMaterial = XMaterial.matchXMaterial(k);
            if (optionalXMaterial.isPresent()) {
                return optionalXMaterial.get().name();
            }

            String possibleType = split[0];
            String possibleId = split[1];

            Material material = Material.matchMaterial(possibleType);
            if (material != null) {
                return material.name();
            }

            String normalizedType = provider.normalize(possibleType).toUpperCase(Locale.ROOT);
            return normalizedType + ":" + possibleId;
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

    /**
     * Check if the item stack is null or air.
     *
     * @param stack the item stack
     * @return true if the stack is null or air, otherwise false
     */
    public static boolean isAir(ItemStack stack) {
        return (stack == null) || (stack.getType() == Material.AIR);
    }

    /**
     * Get the free space of the player's inventory for the item.
     *
     * @param player the player
     * @param item   the item stack
     * @return the amount of the item that can fit into the inventory, capped at the stack amount,
     * or -1 if the inventory has no room for the item
     */
    public static int getFreeSpace(Player player, ItemStack item) {
        int empty = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (empty >= item.getAmount()) break;
            if (isAir(stack)) {
                empty += item.getMaxStackSize();
                continue;
            }
            if (!item.isSimilar(stack)) continue;
            empty += (stack.getMaxStackSize() - stack.getAmount());
        }
        if (empty > 0) return Math.min(empty, item.getAmount());
        return -1;
    }

    /**
     * Check if the stack should be stored into the storage instead of the player's inventory.
     * <p>
     * When {@code onlyStoreWhenInvFull} is disabled, the stack can always be stored.
     * Otherwise, it can only be stored when the inventory cannot hold the whole stack.
     *
     * @param player the player
     * @param item   the item stack
     * @return true if the stack should be stored into the storage, otherwise false
     */
    public static boolean canStore(Player player, ItemStack item) {
        if (!ExtraStorage.getInstance().get(SettingConfig.class).onlyStoreWhenInvFull()) return true;
        return getFreeSpace(player, item) < item.getAmount();
    }

    public static Item getItem(String key) {
        try {
            return itemCache.get(key);
        } catch (ExecutionException e) {
            ExtraStorage.getInstance().getLogger().log(Level.SEVERE, "[ITEM] Error getting item: " + key, e);
            return Item.INVALID;
        }
    }

    @SuppressWarnings("deprecation")
    public static Item getItem(ItemStack item) {
        if (item.hasItemMeta()) return provider.wrap(item);
        try {
            XMaterial material = XMaterial.matchXMaterial(item);
            return new XVanillaItem(material);
        } catch (Exception e) {
            // IGNORED - Modded Material from Hybrid servers detected or New materials XSeries hasn't recognized
        }
        return new VanillaItem(item.getType(), item.getDurability());
    }

    public static ItemType getItemType(Item item) {
        if (!item.isValid()) {
            return ItemType.NONE;
        }
        return item instanceof VanillaItem || item instanceof XVanillaItem ? ItemType.VANILLA : ItemType.CUSTOM;
    }

    public enum ItemType {
        NONE, VANILLA, CUSTOM
    }

    public static final class VanillaItem implements Item {
        private final @Nullable Material material;
        private final @Nullable Short durability;

        public VanillaItem(@Nullable Material material, @Nullable Short durability) {
            this.material = material;
            this.durability = durability;
        }

        public String getName() {
            if (material == null) {
                return null;
            }
            if (durability != null && durability >= 0) {
                return material.name() + ":" + durability;
            }
            return material.name();
        }

        @Override
        public boolean isValid() {
            return material != null;
        }

        @Override
        public @Nullable ItemKey key() {
            String name = getName();
            if (name == null) return null;
            return new ItemKey("minecraft", name);
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

    public static final class XVanillaItem implements Item {
        private final @Nullable XMaterial material;

        public XVanillaItem(@Nullable XMaterial material) {
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
            return new ItemKey("xminecraft", material.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public @Nullable ItemStack bukkitItem() {
            if (material == null) return null;
            return material.parseItem();
        }

        @Override
        public @Nullable ItemStack bukkitItem(@NotNull Player player) {
            return bukkitItem();
        }

        @Override
        public boolean isSimilar(@NotNull ItemStack item) {
            if (material == null) return false;
            return !item.hasItemMeta() && material.isSimilar(item);
        }
    }
}
