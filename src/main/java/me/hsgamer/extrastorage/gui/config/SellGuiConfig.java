package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.common.Config;
import me.hsgamer.extrastorage.configs.converters.StringListConverter;

import java.util.Arrays;
import java.util.List;

@ConfigNode
@Comment("Sell GUI configuration")
public interface SellGuiConfig {

    Config config();

    @ConfigNode
    @Comment("GUI settings")
    interface SettingsConfig {
        @Comment("The title of GUI")
        @ConfigPath(value = "Title", priority = 0)
        default String title() { return "§0sᴇʟʟɪɴɢ ɪᴛᴇᴍs"; }

        @Comment("Rows on GUI (1-6)")
        @ConfigPath(value = "Rows", priority = 10)
        default int rows() { return 6; }

        @Comment(value = {"Default sort type:", "Available: MATERIAL, NAME, QUANTITY and UNFILTERED."})
        @ConfigPath(value = "DefaultSort", priority = 20)
        default String defaultSort() { return "MATERIAL"; }

        @Comment(value = {"Plays a sound when the player interacts on GUI:", "Empty the string to disable this feature."})
        @ConfigPath(value = "Sound", priority = 30)
        default String sound() { return "ui_button_click"; }
    }

    @ConfigNode
    @Comment("This icon represents items that are in the storage")
    interface SellRepresentItemConfig extends ItemConfig {
        @Comment("Empty the string to use the configured items name.")
        @ConfigPath(value = "Name", priority = 0)
        default String name() { return ""; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 10)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&7+ Status: &r{status}",
                    "&7+ You have: &e{quantity}",
                    "",
                    "&3* Price: &b${price}",
                    "&3* Amount: &bx{amount}",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] &7Sell one.",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] &7Sell stack.",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] &7Sell all."
            );
        }

        @ConfigPath(value = "Slots", converter = StringListConverter.class, priority = 20)
        default List<String> slots() { return Arrays.asList("11-17", "20-26", "29-35"); }
    }

    @ConfigNode
    @Comment("Sorting items by material")
    interface SortByMaterialConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "BOOK"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&2→ &aBy material",
                    "&8 ● &7By name",
                    "&8 ● &7By quantity",
                    "&8 ● &7By unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] #dcdde1ʀᴇᴠᴇʀsᴇ ꜰɪʟᴛᴇʀ"
            );
        }

        @ConfigPath(value = "Slot", priority = 95)
        default int slot() { return 51; }
    }

    @ConfigNode
    @Comment("Sorting items by name")
    interface SortByNameConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "BOOK"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&8 ● &7By material",
                    "&2→ &aBy name",
                    "&8 ● &7By quantity",
                    "&8 ● &7By unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] #dcdde1ʀᴇᴠᴇʀsᴇ ꜰɪʟᴛᴇʀ"
            );
        }

        @ConfigPath(value = "Slot", priority = 95)
        default int slot() { return 51; }
    }

    @ConfigNode
    @Comment("Sorting items by quantity")
    interface SortByQuantityConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "BOOK"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&8 ● &7By material",
                    "&8 ● &7By name",
                    "&2→ &aBy quantity",
                    "&8 ● &7By unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] #dcdde1ʀᴇᴠᴇʀsᴇ ꜰɪʟᴛᴇʀ"
            );
        }

        @ConfigPath(value = "Slot", priority = 95)
        default int slot() { return 51; }
    }

    @ConfigNode
    @Comment("Sorting unfiltered items first")
    interface SortByUnfilterConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "BOOK"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&8 ● &7By material",
                    "&8 ● &7By name",
                    "&8 ● &7By quantity",
                    "&2→ &aBy unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ"
            );
        }

        @ConfigPath(value = "Slot", priority = 95)
        default int slot() { return 51; }
    }

    @ConfigNode
    @Comment("Control items for the sell GUI")
    interface SellControlItemsConfig extends ControlItemsConfig {
        @ConfigPath(value = "SortByMaterial", priority = 0)
        SortByMaterialConfig sortByMaterial();

        @ConfigPath(value = "SortByName", priority = 10)
        SortByNameConfig sortByName();

        @ConfigPath(value = "SortByQuantity", priority = 20)
        SortByQuantityConfig sortByQuantity();

        @ConfigPath(value = "SortByUnfilter", priority = 30)
        SortByUnfilterConfig sortByUnfilter();
    }

    @ConfigPath(value = "Settings", priority = 0)
    SettingsConfig settings();

    @ConfigPath(value = "RepresentItem", priority = 10)
    SellRepresentItemConfig representItem();

    @ConfigPath(value = "ControlItems", priority = 20)
    SellControlItemsConfig controlItems();

    @Comment(value = {"These are decorative items, which will make your GUI more beautiful!", "You can add/delete items in this section.", "You can only add 'commands' in this section."})
    @ConfigPath(value = "DecorateItems", priority = 30)
    DecorateItemsConfig decorateItems();
}
