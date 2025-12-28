package me.hsgamer.extrastorage.gui.item;

import io.github.projectunified.craftitem.core.ItemModifier;
import io.github.projectunified.craftitem.spigot.core.SpigotItem;
import io.github.projectunified.craftitem.spigot.core.SpigotItemModifier;
import io.github.projectunified.craftitem.spigot.modifier.LoreModifier;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public interface GUIItemModifier {
    static GUIItemModifier getDisplayItemModifier(ConfigurationSection config, String path, boolean additionalLore) {
        List<ItemModifier> itemModifiers = new ArrayList<>();
        String name = config.getString(path + ".Name");
        if (name != null && !name.isEmpty()) {
            itemModifiers.add((SpigotItemModifier) (item, translator) -> item.editMeta(meta -> meta.setDisplayName(translator.apply(name))));
        }
        List<String> lore = config.getStringList(path + ".Lore");
        if (!lore.isEmpty()) {
            if (additionalLore) {
                itemModifiers.add(
                        (SpigotItemModifier) (item, translator) -> item.editMeta(meta -> {
                            List<String> itemLore = meta.getLore();
                            if (!meta.hasLore() || itemLore == null) {
                                itemLore = new ArrayList<>();
                            }
                            for (String loreLine : lore) {
                                itemLore.add(translator.apply(loreLine));
                            }
                            meta.setLore(itemLore);
                        })
                );
            } else {
                itemModifiers.add(
                        new LoreModifier(lore)
                );
            }
        }
        return (spigotItem, translator) -> {
            for (ItemModifier itemModifier : itemModifiers) {
                itemModifier.modify(spigotItem, translator);
            }
        };
    }

    void modify(SpigotItem spigotItem, UnaryOperator<String> translator);

    default ItemStack construct(io.github.projectunified.uniitem.api.Item item, String key, UnaryOperator<String> translator) {
        ItemStack iStack = item.bukkitItem();
        assert iStack != null;
        SpigotItem spigotItem = new SpigotItem(iStack);
        modify(spigotItem, translator);
        spigotItem.editMeta(meta -> {
            if (!meta.hasDisplayName()) {
                meta.setDisplayName(ExtraStorage.getInstance().getSetting().getNameFormatted(key, true));
            }
        });
        return spigotItem.getItemStack();
    }

    default ItemStack construct(Item item, UnaryOperator<String> translator) {
        ItemStack iStack = item.getItem();
        SpigotItem spigotItem = new SpigotItem(iStack);
        modify(spigotItem, translator);
        spigotItem.editMeta(meta -> {
            if (!meta.hasDisplayName()) {
                meta.setDisplayName(ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true));
            }
        });
        return spigotItem.getItemStack();
    }
}
