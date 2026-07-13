package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.common.Config;
import me.hsgamer.extrastorage.configs.converters.StringListConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ConfigNode
@Comment("Whitelist GUI configuration")
public interface WhitelistGuiConfig {

    Config config();

    @ConfigNode
    @Comment("GUI settings")
    interface SettingsConfig {
        @Comment("The title of GUI")
        @ConfigPath(value = "Title", priority = 0)
        default String title() { return "§0ᴡʜɪᴛᴇʟɪꜱᴛ ꜰɪʟᴛᴇʀ"; }

        @Comment("Rows on GUI (1-6)")
        @ConfigPath(value = "Rows", priority = 10)
        default int rows() { return 6; }

        @Comment(value = {"Plays a sound when the player interacts on GUI:", "Empty the string to disable this feature."})
        @ConfigPath(value = "Sound", priority = 30)
        default String sound() { return "ui_button_click"; }
    }

    @ConfigNode
    @Comment("This icon represents items are whitelisted")
    interface WhitelistRepresentItemConfig extends ItemConfig {
        @ConfigPath(value = "Name", priority = 0)
        default String name() { return ""; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 10)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&c* By removing this item, it",
                    "&cwill no longer be picked up",
                    "&cto the player''s storage.",
                    "",
                    "&8[&6Click&8] &7Remove this item."
            );
        }

        @ConfigPath(value = "Slots", converter = StringListConverter.class, priority = 20)
        default List<String> slots() { return Arrays.asList("11-17", "20-26", "29-35"); }
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
                    "&2→ &aSort by name",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] &7Sorting A - Z",
                    "&8[&6R-Click&8] &7Sorting Z - A"
            );
        }

        @ConfigPath(value = "Slot", priority = 95)
        default int slot() { return 50; }
    }

    @ConfigNode
    @Comment("Control items for the whitelist GUI")
    interface WhitelistControlItemsConfig {
        @Comment("Back to previous page")
        @ConfigPath(value = "PreviousPage", priority = 10)
        ItemConfig previousPage();

        @Comment("Go to next page")
        @ConfigPath(value = "NextPage", priority = 20)
        ItemConfig nextPage();

        @ConfigPath(value = "SortByName", priority = 0)
        SortByNameConfig sortByName();
    }

    @ConfigPath(value = "Settings", priority = 0)
    SettingsConfig settings();

    @ConfigPath(value = "RepresentItem", priority = 10)
    WhitelistRepresentItemConfig representItem();

    @ConfigPath(value = "ControlItems", priority = 20)
    WhitelistControlItemsConfig controlItems();

    @Comment(value = {"These are decorative items, which will make your GUI more beautiful!", "You can add/delete items in this section."})
    @ConfigPath(value = "DecorateItems", priority = 30)
    DecorateItemsConfig decorateItems();
}
