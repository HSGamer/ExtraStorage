package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import me.hsgamer.extrastorage.configs.converters.StringListConverter;

import java.util.Collections;
import java.util.List;

@ConfigNode
@Comment("Item configuration for GUI elements")
public interface ItemConfig {
    @Comment(value = {"For those who prefer to use custom models from ItemsAdder (or Oraxen),", "use this option to specify a model.", "Format: 'Oraxen:<id>' or 'IA:<namespaceId>'", "If this option is used, it means that: Material and Data cannot be used.", "Remember to remove the 'CustomModelData' option first.", "Leave it blank will disable this feature."})
    @ConfigPath(value = "Model", priority = 0)
    default String model() { return ""; }

    @Comment(value = {"Using 'Texture' option if you want to display the head texture (requires Material is PLAYER_HEAD):", "Texture: '<value>' #Can be found at: https://minecraft-heads.com/ (Value field).", "Texture: 'hdb-<id>' #Using for HeadDatabase plugin."})
    @ConfigPath(value = "Texture", priority = 10)
    default String texture() { return ""; }

    @Comment("The Bukkit material for this item")
    @ConfigPath(value = "Material", priority = 20)
    default String material() { return "STONE"; }

    @Comment("Item stack amount (may not need to configure)")
    @ConfigPath(value = "Amount", priority = 30)
    default int amount() { return 1; }

    @Comment("Material data value (may not need to configure)")
    @ConfigPath(value = "Data", priority = 40)
    default int data() { return 0; }

    @Comment("Can only be used on the server version 1.14+. May not need to configure.")
    @ConfigPath(value = "CustomModelData", priority = 50)
    default int customModelData() { return 0; }

    @Comment("Namespaced key for the item model (1.21+)")
    @ConfigPath(value = "ItemModel", priority = 55)
    default String itemModel() { return ""; }

    @Comment("Item flags to hide tooltip parts")
    @ConfigPath(value = "HideFlags", converter = StringListConverter.class, priority = 60)
    default List<String> hideFlags() { return Collections.emptyList(); }

    @Comment("Enchantment strings (comma-separated)")
    @ConfigPath(value = "Enchantments", converter = StringListConverter.class, priority = 65)
    default List<String> enchantments() { return Collections.emptyList(); }

    @Comment("Display name of the item (supports color codes)")
    @ConfigPath(value = "Name", priority = 70)
    default String name() { return ""; }

    @Comment("Lore lines of the item (supports color codes)")
    @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
    default List<String> lore() { return Collections.emptyList(); }

    @Comment("List of slot positions for this item (e.g. '11-17', '20-26')")
    @ConfigPath(value = "Slots", converter = StringListConverter.class, priority = 90)
    default List<String> slots() { return Collections.emptyList(); }

    @Comment("Single slot position (1-indexed, use -1 to hide)")
    @ConfigPath(value = "Slot", priority = 95)
    default int slot() { return -1; }

    @Comment("Commands to execute when this item is clicked")
    @ConfigPath(value = "Commands", converter = StringListConverter.class, priority = 100)
    default List<String> commands() { return Collections.emptyList(); }
}
