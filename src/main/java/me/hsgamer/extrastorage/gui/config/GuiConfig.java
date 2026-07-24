package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import me.hsgamer.extrastorage.configs.converters.MapConverter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface GuiConfig {
    void reloadConfig();

    SettingsConfig settings();

    Map<String, Object> representItem();

    GuiConfig.ControlItemsConfig controlItems();

    Map<String, Map<String, Object>> decorateItems();

    interface SettingsConfig {
        String title();

        int rows();

        String sound();

        default String defaultSort() {
            return null;
        }
    }

    interface ControlItemsConfig {
        @Comment("Back to previous page:")
        @ConfigPath(value = "PreviousPage", priority = 10, converter = MapConverter.class)
        default Map<String, Object> previousPage() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "ARROW");
            map.put("Name", "&8[&6ᴄʟɪᴄᴋ&8] #dcdde1ᴘʀᴇᴠɪᴏᴜs ᴘᴀɢᴇ");
            map.put("Lore", Collections.singletonList("&7ᴄᴜʀʀᴇɴᴛ ᴘᴀɢᴇ: &f{page}&7/&c{max_pages}"));
            map.put("Slot", 48);
            return map;
        }

        @Comment("Go to next page:")
        @ConfigPath(value = "NextPage", priority = 20, converter = MapConverter.class)
        default Map<String, Object> nextPage() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Material", "ARROW");
            map.put("Name", "&8[&6ᴄʟɪᴄᴋ&8] #dcdde1ɴᴇxᴛ ᴘᴀɢᴇ");
            map.put("Lore", Collections.singletonList("&7ᴄᴜʀʀᴇɴᴛ ᴘᴀɢᴇ: &f{page}&7/&c{max_pages}"));
            map.put("Slot", 52);
            return map;
        }
    }
}
