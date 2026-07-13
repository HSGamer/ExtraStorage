package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.common.Config;
import me.hsgamer.extrastorage.configs.converters.StringListConverter;

import java.util.Arrays;
import java.util.List;

@ConfigNode
@Comment("Partner GUI configuration")
public interface PartnerGuiConfig {

    Config config();

    @ConfigNode
    @Comment("GUI settings")
    interface SettingsConfig {
        @Comment("The title of GUI")
        @ConfigPath(value = "Title", priority = 0)
        default String title() { return "§0sᴛᴏʀᴀɢᴇ ᴘᴀʀᴛɴᴇʀ"; }

        @Comment("Rows on GUI (1-6)")
        @ConfigPath(value = "Rows", priority = 10)
        default int rows() { return 6; }

        @Comment(value = {"Default sort type:", "Available: NAME and TIME."})
        @ConfigPath(value = "DefaultSort", priority = 20)
        default String defaultSort() { return "NAME"; }

        @Comment(value = {"Plays a sound when the player interacts on GUI:", "Empty the string to disable this feature."})
        @ConfigPath(value = "Sound", priority = 30)
        default String sound() { return "ui_button_click"; }
    }

    @ConfigNode
    @Comment("This icon represents the player's partners")
    interface PartnerRepresentItemConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "PLAYER_HEAD"; }

        @ConfigPath(value = "Amount", priority = 30)
        default int amount() { return 1; }

        @ConfigPath(value = "Data", priority = 40)
        default int data() { return 0; }

        @ConfigPath(value = "CustomModelData", priority = 50)
        default int customModelData() { return 0; }

        @Comment(value = {"Using 'Texture' option if you want to display the head texture", "(requires Material is PLAYER_HEAD):", "Texture: '%partner%' - Displaying the partner's skull."})
        @ConfigPath(value = "Texture", priority = 10)
        default String texture() { return "%partner%"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "&fPartner: &6{partner}"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&7+ Timestamp: &f{time}",
                    "",
                    "&8[&6ᴄʟɪᴄᴋ&8] &7Remove this partner",
                    "&7from your list."
            );
        }

        @ConfigPath(value = "Slots", converter = StringListConverter.class, priority = 90)
        default List<String> slots() { return Arrays.asList("11-17", "20-26", "29-35"); }
    }

    @ConfigNode
    @Comment("Sorting items by partner name")
    interface SortByNameConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "BOOK"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "&fSorting Partners"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&2→ &aPartner''s name",
                    "&8 ● &7Timestamp",
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
    @Comment("Sorting items by timestamp")
    interface SortByTimeConfig extends ItemConfig {
        @ConfigPath(value = "Material", priority = 20)
        default String material() { return "BOOK"; }

        @ConfigPath(value = "Name", priority = 70)
        default String name() { return "&fSorting Partners"; }

        @ConfigPath(value = "Lore", converter = StringListConverter.class, priority = 80)
        default List<String> lore() {
            return Arrays.asList(
                    "",
                    "&8 ● &7Partner''s name",
                    "&2→ &aTimestamp",
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
    @Comment("Control items for the partner GUI")
    interface PartnerControlItemsConfig extends ControlItemsConfig {
        @ConfigPath(value = "SortByName", priority = 0)
        SortByNameConfig sortByName();

        @ConfigPath(value = "SortByTime", priority = 30)
        SortByTimeConfig sortByTime();
    }

    @ConfigPath(value = "Settings", priority = 0)
    SettingsConfig settings();

    @ConfigPath(value = "RepresentItem", priority = 10)
    PartnerRepresentItemConfig representItem();

    @ConfigPath(value = "ControlItems", priority = 20)
    PartnerControlItemsConfig controlItems();

    @Comment(value = {"These are decorative items, which will make your GUI more beautiful!", "You can add/delete items in this section.", "You can only add 'commands' in this section."})
    @ConfigPath(value = "DecorateItems", priority = 30)
    DecorateItemsConfig decorateItems();
}
