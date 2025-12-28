package me.hsgamer.extrastorage.gui.item;

import com.google.common.base.Strings;
import io.github.projectunified.craftitem.core.ItemModifier;
import io.github.projectunified.craftitem.spigot.core.SpigotItem;
import io.github.projectunified.craftitem.spigot.core.SpigotItemModifier;
import io.github.projectunified.craftitem.spigot.modifier.EnchantmentModifier;
import io.github.projectunified.craftitem.spigot.modifier.ItemFlagModifier;
import io.github.projectunified.craftitem.spigot.modifier.LoreModifier;
import io.github.projectunified.craftitem.spigot.skull.SkullModifier;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface GUIItem {
    Pattern HDB_PATTERN = Pattern.compile("(?ium)(hdb)-(?<value>[a-zA-Z0-9]+)");

    static GUIItem get(
            String model,
            String materialName,
            String name,
            List<String> lore,
            Integer customModelData,
            int amount,
            String texture,
            List<String> enchants,
            List<String> flags,
            BiConsumer<User, ItemMeta> meta
    ) {
        Function<User, SpigotItem> spigotItemSupplier;
        List<ItemModifier> itemModifiers = new ArrayList<>();
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
                        new SkullModifier(user.getTexture()).modify(spigotItem);
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
                        HeadDatabaseAPI api = new HeadDatabaseAPI();
                        spigotItemSupplier = user -> {
                            ItemStack itemStack = api.getItemHead(ID);
                            if (itemStack != null) {
                                if (itemStack.hasItemMeta()) {
                                    ItemMeta itemMeta = itemStack.getItemMeta();
                                    assert itemMeta != null;
                                    itemMeta.setDisplayName(null);
                                    itemMeta.setLore(null);
                                    itemStack.setItemMeta(itemMeta);
                                }
                            } else {
                                itemStack = new ItemStack(Material.PLAYER_HEAD);
                            }
                            return new SpigotItem(itemStack);
                        };
                    }
                } else {
                    spigotItemSupplier = user -> new SpigotItem(new ItemStack(Material.PLAYER_HEAD));
                    itemModifiers.add(new SkullModifier(texture));
                }
            }
        } else {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                spigotItemSupplier = user -> new SpigotItem(new ItemStack(Material.STONE));
            } else {
                spigotItemSupplier = user -> new SpigotItem(new ItemStack(material));
            }
        }

        itemModifiers.add((item, translator) -> item.setAmount(Math.max(1, amount)));
        if (customModelData != null) {
            itemModifiers.add((SpigotItemModifier) (item, translator) -> item.editMeta(meta1 -> meta1.setCustomModelData(customModelData)));
        }
        if (flags != null) {
            itemModifiers.add(new ItemFlagModifier(flags));
        }
        if (enchants != null) {
            itemModifiers.add(new EnchantmentModifier(enchants, ','));
        }
        if (name != null && !name.isEmpty()) {
            itemModifiers.add((SpigotItemModifier) (item, translator) -> item.editMeta(meta1 -> meta1.setDisplayName(translator.apply(name))));
        }
        if (lore != null && !lore.isEmpty()) {
            itemModifiers.add(new LoreModifier(lore));
        }

        return (user, translator) -> {
            SpigotItem spigotItem = spigotItemSupplier.apply(user);
            for (ItemModifier itemModifier : itemModifiers) {
                itemModifier.modify(spigotItem, translator);
            }
            if (meta != null) {
                spigotItem.editMeta(meta1 -> meta.accept(user, meta1));
            }
            return spigotItem.getItemStack();
        };
    }

    static GUIItem get(ConfigurationSection config, String path, BiConsumer<User, ItemMeta> meta) {
        return get(
                config.getString(path + ".Model"),
                config.getString(path + ".Material"),
                config.getString(path + ".Name"),
                config.getStringList(path + ".Lore"),
                config.contains(path + ".CustomModelData") ? config.getInt(path + ".CustomModelData") : null,
                config.getInt(path + ".Amount"),
                config.getString(path + ".Texture"),
                config.getStringList(path + ".Enchantments"),
                config.getStringList(path + ".HideFlags"),
                meta
        );
    }

    ItemStack getItem(User user, UnaryOperator<String> translator);
}
