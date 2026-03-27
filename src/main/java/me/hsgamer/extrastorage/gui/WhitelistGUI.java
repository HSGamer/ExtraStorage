package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class WhitelistGUI extends BaseGUI<WhitelistGUI.SortType> {

    public WhitelistGUI(Player player) {
        super(player, ExtraStorage.getInstance().getWhitelistGuiConfig(), SortType.class);

        setup();
    }


    @Override
    protected boolean onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getWhoClicked().getOpenInventory().getBottomInventory()) {
            final ItemStack item = event.getCurrentItem();
            if ((item == null) || (item.getType() == Material.AIR)) return false;

            final String validKey = ItemUtil.toMaterialKey(item);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return false;
            }
            Setting setting = ExtraStorage.getInstance().getSetting();
            if (setting.getBlacklist().contains(validKey)) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return false;
            }
            if (setting.getWhitelist().contains(validKey)) {
                player.sendMessage(Message.getMessage("FAIL.item-already-whitelisted"));
                return false;
            }

            setting.addToWhitelist(validKey);
            for (User user : ExtraStorage.getInstance().getUserManager().getUsers()) {
                Storage storage = user.getStorage();
                Optional<Item> optional = storage.getItem(validKey);
                if (!optional.isPresent()) storage.addNewItem(validKey);
            }

            player.sendMessage(Message.getMessage("SUCCESS.item-added-to-whitelist").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(validKey, true)));

            updateRepresentItems();
            update();
        }
        return super.onClick(event);
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        Setting setting = ExtraStorage.getInstance().getSetting();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        List<String> whitelist = new ArrayList<>(setting.getWhitelist());

        if (sort == SortType.NAME_REVERSE) {
            whitelist.sort(Comparator.reverseOrder());
        } else {
            whitelist.sort(Comparator.naturalOrder());
        }

        return whitelist.stream()
                .map(key -> {
                    io.github.projectunified.uniitem.api.Item item = ItemUtil.getItem(key);
                    if (!item.isValid()) return null;
                    ItemStack iStack = displayModifier.construct(item, key, UnaryOperator.identity());

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            setting.removeFromWhitelist(key);

                            for (User user : ExtraStorage.getInstance().getUserManager().getUsers()) {
                                Storage storage = user.getStorage();
                                Optional<Item> optional = storage.getItem(key);
                                if (optional.isPresent()) storage.unfilter(key);
                            }
                            player.sendMessage(Message.getMessage("SUCCESS.item-removed-from-whitelist").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

                            updateRepresentItems();
                            update();
                        });
                        return true;
                    };
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    protected Mask getControlItems(ConfigurationSection section) {
        HybridMask mask = new HybridMask();

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.NAME_NATURAL, section, "SortByName");
        putSortConfig(sortConfigMap, SortType.NAME_REVERSE, section, "SortByName");
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    private void putSortConfig(Map<SortType, SortButtonConfig<SortType>> map, SortType type, ConfigurationSection section, String key) {
        ConfigurationSection subSection = section.getConfigurationSection(key);
        if (subSection == null) return;
        map.put(type, new SortButtonConfig<>(GUIItem.get(subSection, null), getSlots(subSection)));
    }

    public enum SortType {
        NAME_NATURAL, NAME_REVERSE
    }
}
