package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import me.hsgamer.extrastorage.configs.converters.MapConverter;
import me.hsgamer.extrastorage.configs.converters.MapMapConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigNode
@Comment("Sell GUI configuration")
public interface SellGuiConfig extends GuiConfig {

    @Comment("GUI settings")
    @ConfigPath(value = "Settings", priority = 0)
    SellSettingsConfig settings();

    @Comment("This icon represents items that are in the storage.")
    @ConfigPath(value = "RepresentItem", priority = 10, converter = MapConverter.class)
    default Map<String, Object> representItem() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Name", "");
        map.put("Lore", java.util.Arrays.asList(
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
        ));
        map.put("Slots", java.util.Arrays.asList("11-17", "20-26", "29-35"));
        return map;
    }

    @Comment(value = {"Please do not delete any items in this section.", "If you don't want to display these items on GUI, just set their slot to -1."})
    @ConfigPath(value = "ControlItems", priority = 20)
    SellControlItemsConfig controlItems();

    @Comment(value = {"These are decorative items, which will make your GUI more beautiful!", "You can add/delete items in this section.", "You can only add \"commands\" in this section."})
    @ConfigPath(value = "DecorateItems", priority = 30, converter = MapMapConverter.class)
    default Map<String, Map<String, Object>> decorateItems() {
        Map<String, Map<String, Object>> items = new LinkedHashMap<>();
        Map<String, Object> border = new LinkedHashMap<>();
        border.put("Material", "BLACK_STAINED_GLASS_PANE");
        border.put("Name", " ");
        border.put("Slots", java.util.Arrays.asList("0-10", "18", "19", "27", "28", "36", "37", "45", "46-48", "52-54"));
        items.put("border", border);
        Map<String, Object> divider = new LinkedHashMap<>();
        divider.put("Material", "GRAY_STAINED_GLASS_PANE");
        divider.put("Name", " ");
        divider.put("Slots", Collections.singletonList("38-44"));
        items.put("divider", divider);
        return items;
    }

    @ConfigNode
    interface SellSettingsConfig extends SettingsConfig {
        @Comment("The title of GUI:")
        @ConfigPath(value = "Title", priority = 0)
        default String title() {
            return "§0sᴇʟʟɪɴɢ ɪᴛᴇᴍs";
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

        @Comment(value = {"Default sort type:", "* Available: MATERIAL, NAME, QUANTITY and UNFILTERED."})
        @ConfigPath(value = "DefaultSort", priority = 15)
        default String defaultSort() {
            return "MATERIAL";
        }
    }

    @ConfigNode
    @Comment("Control items for the sell GUI")
    interface SellControlItemsConfig extends ControlItemsConfig {
        @Comment("This item is used to display the user's storage information:")
        @ConfigPath(value = "About", priority = 0, converter = MapConverter.class)
        default Map<String, Object> about() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "PAPER");
            map.put("Amount", 1);
            map.put("Data", 0);
            map.put("Texture", "%player%");
            map.put("CustomModelData", 0);
            map.put("Name", "#dcdde1ᴘʟᴀʏᴇʀ sᴛᴏʀᴀɢᴇ ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ");
            map.put("Lore", Arrays.asList(
                    "",
                    "&7+ Owner: &b{player}",
                    "",
                    "&7+ Status: &r{status}",
                    "",
                    "&7+ Space: &e{space}",
                    "",
                    "&7+ Used space: &f{used_space} &7/ &c{used_percent}",
                    "",
                    "&7+ Free space: &f{free_space} &7/ &c{free_percent}"
            ));
            map.put("Slot", 50);
            return map;
        }

        @Comment("Sorting items by material:")
        @ConfigPath(value = "SortByMaterial", priority = 40, converter = MapConverter.class)
        default Map<String, Object> sortByMaterial() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "BOOK");
            map.put("Name", "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ");
            map.put("Lore", java.util.Arrays.asList(
                    "",
                    "&2→ &aBy material",
                    "&8 ● &7By name",
                    "&8 ● &7By quantity",
                    "&8 ● &7By unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] #dcdde1ʀᴇᴠᴇʀsᴇ ꜰɪʟᴛᴇʀ"
            ));
            map.put("Slot", 51);
            return map;
        }

        @Comment("Sorting items by name:")
        @ConfigPath(value = "SortByName", priority = 50, converter = MapConverter.class)
        default Map<String, Object> sortByName() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "BOOK");
            map.put("Name", "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ");
            map.put("Lore", java.util.Arrays.asList(
                    "",
                    "&8 ● &7By material",
                    "&2→ &aBy name",
                    "&8 ● &7By quantity",
                    "&8 ● &7By unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] #dcdde1ʀᴇᴠᴇʀsᴇ ꜰɪʟᴛᴇʀ"
            ));
            map.put("Slot", 51);
            return map;
        }

        @Comment("Sorting items by quantity:")
        @ConfigPath(value = "SortByQuantity", priority = 60, converter = MapConverter.class)
        default Map<String, Object> sortByQuantity() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "BOOK");
            map.put("Name", "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ");
            map.put("Lore", java.util.Arrays.asList(
                    "",
                    "&8 ● &7By material",
                    "&8 ● &7By name",
                    "&2→ &aBy quantity",
                    "&8 ● &7By unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ",
                    "&8[&6s.ᴄʟɪᴄᴋ&8] #dcdde1ʀᴇᴠᴇʀsᴇ ꜰɪʟᴛᴇʀ"
            ));
            map.put("Slot", 51);
            return map;
        }

        @Comment("Sorting unfiltered items first:")
        @ConfigPath(value = "SortByUnfilter", priority = 70, converter = MapConverter.class)
        default Map<String, Object> sortByUnfilter() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "BOOK");
            map.put("Name", "#dcdde1sᴏʀᴛ sᴛᴏʀᴀɢᴇ ᴄᴏɴᴛᴇɴᴛ");
            map.put("Lore", java.util.Arrays.asList(
                    "",
                    "&8 ● &7By material",
                    "&8 ● &7By name",
                    "&8 ● &7By quantity",
                    "&2→ &aBy unfiltered",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ꜰɪʟᴛᴇʀ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. ꜰɪʟᴛᴇʀ"
            ));
            map.put("Slot", 51);
            return map;
        }

        @Comment("Switching button between GUIs")
        @ConfigPath(value = "SwitchGui", priority = 30, converter = MapConverter.class)
        default Map<String, Object> switchGui() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "CHEST_MINECART");
            map.put("Name", "#dcdde1sᴡɪᴛᴄʜ ᴛᴏ ᴏᴛʜᴇʀ sᴛᴏʀᴀɢᴇ");
            map.put("Lore", Arrays.asList(
                    "",
                    "&8 ● &7Partners",
                    "&8 ● &7Filter",
                    "&8 ● &7Storage",
                    "&2→ &aSelling",
                    "",
                    "&8[&6ʟ.ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ sᴛᴏʀᴀɢᴇ",
                    "&8[&6ʀ.ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠ. sᴛᴏʀᴀɢᴇ"
            ));
            map.put("Slot", 49);
            return map;
        }
    }
}
