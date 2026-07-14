package me.hsgamer.extrastorage.gui.item;

import com.google.common.base.Strings;
import io.github.projectunified.craftitem.core.ItemModifier;
import io.github.projectunified.craftitem.spigot.core.SpigotItem;
import io.github.projectunified.craftitem.spigot.core.SpigotItemModifier;
import io.github.projectunified.craftitem.spigot.modifier.EnchantmentModifier;
import io.github.projectunified.craftitem.spigot.modifier.ItemFlagModifier;
import io.github.projectunified.craftitem.spigot.skull.SkullModifier;
import io.github.projectunified.craftux.common.ActionItem;
import io.github.projectunified.uniitem.headdatabase.HeadDatabaseItem;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.gui.util.CompatItemUtil;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GUIItem {
    Pattern HDB_PATTERN = Pattern.compile("(?ium)(hdb)-(?<value>[a-zA-Z0-9]+)");

    static GUIItem get(Map<String, Object> itemConfig, BiConsumer<User, ItemMeta> meta) {
        Function<User, SpigotItem> spigotItemSupplier;
        List<ItemModifier> itemModifiers = new ArrayList<>();

        String model = (String) itemConfig.getOrDefault("Model", "");
        String texture = (String) itemConfig.getOrDefault("Texture", "");
        if (!Strings.isNullOrEmpty(model)) {
            io.github.projectunified.uniitem.api.Item item = ItemUtil.getItem(model);
            spigotItemSupplier = user -> {
                ItemStack itemStack = item.tryBukkitItem(user.getPlayer());
                if (itemStack == null) {
                    itemStack = new ItemStack(Material.STONE);
                } else {
                    itemStack = itemStack.clone();
                }
                return new SpigotItem(itemStack);
            };
        } else if (!Strings.isNullOrEmpty(texture)) {
            if (texture.matches(Utils.getRegex("viewer", "player"))) {
                spigotItemSupplier = user -> {
                    SpigotItem spigotItem = new SpigotItem(new ItemStack(Material.PLAYER_HEAD));
                    if (user != null) {
                        String userTexture = user.getTexture();
                        SkullModifier skullModifier = new SkullModifier(userTexture.isEmpty() || user.isOnline() ? user.getUUID().toString() : userTexture);
                        skullModifier.modify(spigotItem);
                    }
                    return spigotItem;
                };
            } else {
                Matcher matcher = HDB_PATTERN.matcher(texture);
                if (matcher.find()) {
                    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("HeadDatabase")) {
                        spigotItemSupplier = user -> new SpigotItem(new ItemStack(Material.PLAYER_HEAD));
                    } else {
                        String ID = matcher.group("value");
                        HeadDatabaseItem headDatabaseItem = new HeadDatabaseItem(ID);
                        spigotItemSupplier = user -> {
                            ItemStack item = headDatabaseItem.bukkitItem();
                            if (item == null) {
                                item = new ItemStack(Material.PLAYER_HEAD);
                            }
                            return new SpigotItem(item);
                        };
                    }
                } else {
                    spigotItemSupplier = user -> new SpigotItem(new ItemStack(Material.PLAYER_HEAD));
                    itemModifiers.add(new SkullModifier(texture));
                }
            }
        } else {
            String materialName = (String) itemConfig.getOrDefault("Material", "STONE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                spigotItemSupplier = user -> new SpigotItem(new ItemStack(Material.STONE));
            } else {
                spigotItemSupplier = user -> new SpigotItem(new ItemStack(material));
            }
        }

        int amount = itemConfig.containsKey("Amount") ? ((Number) itemConfig.get("Amount")).intValue() : 1;
        itemModifiers.add((item, translator) -> item.setAmount(Math.max(1, amount)));

        int customModelData = itemConfig.containsKey("CustomModelData") ? ((Number) itemConfig.get("CustomModelData")).intValue() : 0;
        if (customModelData != 0) {
            itemModifiers.add((SpigotItemModifier) (item, translator) -> item.editMeta(meta1 -> CompatItemUtil.setCustomModelData(meta1, customModelData)));
        }

        String itemModel = (String) itemConfig.getOrDefault("ItemModel", "");
        if (!Strings.isNullOrEmpty(itemModel)) {
            itemModifiers.add((SpigotItemModifier) (item, translator) -> item.editMeta(meta1 -> CompatItemUtil.setItemModel(meta1, itemModel)));
        }

        List<String> flags = itemConfig.get("HideFlags") instanceof List ? (List<String>) itemConfig.get("HideFlags") : Collections.emptyList();
        if (!flags.isEmpty()) {
            itemModifiers.add(new ItemFlagModifier(flags));
        }

        List<String> enchants = itemConfig.get("Enchantments") instanceof List ? (List<String>) itemConfig.get("Enchantments") : Collections.emptyList();
        if (!enchants.isEmpty()) {
            itemModifiers.add(new EnchantmentModifier(enchants, ','));
        }

        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(itemConfig, false);

        return (user, translator) -> {
            SpigotItem spigotItem = spigotItemSupplier.apply(user);
            for (ItemModifier itemModifier : itemModifiers) {
                itemModifier.modify(spigotItem, translator);
            }
            displayModifier.modify(spigotItem, translator);
            if (meta != null) {
                spigotItem.editMeta(meta1 -> meta.accept(user, meta1));
            }
            return spigotItem.getItemStack();
        };
    }

    ItemStack getItem(User user, UnaryOperator<String> translator);

    default void apply(ActionItem actionItem, User user, UnaryOperator<String> translator) {
        actionItem.setItem(getItem(user, translator));
    }
}
