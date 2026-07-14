package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import me.hsgamer.extrastorage.configs.converters.MapConverter;
import me.hsgamer.extrastorage.configs.converters.MapMapConverter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigNode
@Comment("Whitelist GUI configuration")
public interface WhitelistGuiConfig extends GuiConfig {

    @Comment("This icon represents items are whitelisted.")
    @ConfigPath(value = "RepresentItem", priority = 10, converter = MapConverter.class)
    default Map<String, Object> representItem() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Name", "");
        map.put("Lore", java.util.Arrays.asList(
                "",
                "&c* By removing this item, it",
                "&cwill no longer be picked up",
                "&cto the player''s storage.",
                "",
                "&8[&6Click&8] &7Remove this item."
        ));
        map.put("Slots", java.util.Arrays.asList("11-17", "20-26", "29-35"));
        return map;
    }

    @Comment("GUI settings")
    @ConfigPath(value = "Settings", priority = 0)
    WhitelistSettingsConfig settings();

    @Comment(value = {"Please do not delete any items in this section.", "If you don't want to display these items on GUI, just set their slot to -1."})
    @ConfigPath(value = "ControlItems", priority = 20)
    WhitelistControlItemsConfig controlItems();

    @Comment(value = {"These are decorative items, which will make your GUI more beautiful!", "You can add/delete items in this section."})
    @ConfigPath(value = "DecorateItems", priority = 30, converter = MapMapConverter.class)
    default Map<String, Map<String, Object>> decorateItems() {
        Map<String, Map<String, Object>> items = new LinkedHashMap<>();
        Map<String, Object> border = new LinkedHashMap<>();
        border.put("Material", "BLACK_STAINED_GLASS_PANE");
        border.put("Name", " ");
        border.put("Slots", java.util.Arrays.asList("0-10", "18", "19", "27", "28", "36", "37", "45-49", "51-54"));
        items.put("border", border);
        Map<String, Object> divider = new LinkedHashMap<>();
        divider.put("Material", "GRAY_STAINED_GLASS_PANE");
        divider.put("Name", " ");
        divider.put("Slots", Collections.singletonList("38-44"));
        items.put("divider", divider);
        return items;
    }

    @ConfigNode
    interface WhitelistSettingsConfig extends SettingsConfig {
        @Comment("The title of GUI:")
        @ConfigPath(value = "Title", priority = 0)
        default String title() {
            return "§0ᴡʜɪᴛᴇʟɪꜱᴛ ꜰɪʟᴛᴇʀ";
        }

        @Comment(value = {"Rows on GUI:", "* The value can only be from 1 to 6."})
        @ConfigPath(value = "Rows", priority = 10)
        default int rows() {
            return 6;
        }

        @Comment(value = {"Plays a sound when the player interacts on GUI:", "Empty the string (like below) will disable this feature."})
        @ConfigPath(value = "Sound", priority = 20)
        default String sound() {
            return "ui_button_click";
        }
    }

    @ConfigNode
    @Comment("Control items for the whitelist GUI")
    interface WhitelistControlItemsConfig extends ControlItemsConfig {
        @Comment("Sorting items by name:")
        @ConfigPath(value = "SortByName", priority = 40, converter = MapConverter.class)
        default Map<String, Object> sortByName() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "BOOK");
            map.put("Name", "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ");
            map.put("Lore", java.util.Arrays.asList(
                    "",
                    "&2→ &aSort by name",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] &7Sorting A - Z",
                    "&8[&6R-Click&8] &7Sorting Z - A"
            ));
            map.put("Slot", 50);
            return map;
        }

        @Comment("Back to previous page:")
        @ConfigPath(value = "PreviousPage", priority = 10, converter = MapConverter.class)
        default Map<String, Object> previousPage() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "ARROW");
            map.put("Name", "&8[&6ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠɪᴏᴜs ᴘᴀɢᴇ");
            map.put("Lore", java.util.Collections.singletonList("&7ᴄᴜʀʀᴇɴᴛ ᴘᴀɢᴇ: &f{page}&7/&c{max_pages}"));
            map.put("Slots", Collections.singletonList("49"));
            return map;
        }

        @Comment("Go to next page:")
        @ConfigPath(value = "NextPage", priority = 20, converter = MapConverter.class)
        default Map<String, Object> nextPage() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "ARROW");
            map.put("Name", "&8[&6ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ᴘᴀɢᴇ");
            map.put("Lore", java.util.Collections.singletonList("&7ᴄᴜʀʀᴇɴᴛ ᴘᴀɢᴇ: &f{page}&7/&c{max_pages}"));
            map.put("Slots", Collections.singletonList("51"));
            return map;
        }
    }
}
