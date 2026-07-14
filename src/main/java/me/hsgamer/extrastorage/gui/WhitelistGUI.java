package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.SettingConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.config.WhitelistGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class WhitelistGUI extends BaseGUI<WhitelistGUI.SortType, WhitelistGuiConfig> {

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
                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().invalidItem()));
                return false;
            }
            SettingConfig setting = ExtraStorage.getInstance().getSetting();
            if (setting.getNormalizedBlacklist().contains(validKey)) {
                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemBlacklisted()));
                return false;
            }
            if (setting.getNormalizedWhitelist().contains(validKey)) {
                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemAlreadyWhitelisted()));
                return false;
            }

            setting.addToWhitelist(validKey);
            for (User user : ExtraStorage.getInstance().getUserManager().getUsers()) {
                Storage storage = user.getStorage();
                Optional<Item> optional = storage.getItem(validKey);
                if (!optional.isPresent()) storage.addNewItem(validKey);
            }

            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().itemAddedToWhitelist()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(validKey, true)));

            updateRepresentItems();
            update();
        }
        return super.onClick(event);
    }

    @Override
    protected List<Button> getRepresentItems(Map<String, Object> section) {
        SettingConfig setting = ExtraStorage.getInstance().getSetting();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        List<String> whitelist = new ArrayList<>(setting.getNormalizedWhitelist());

        Comparator<String> comparator;
        switch (sort) {
            case NAME_REVERSE:
                comparator = SortUtil.compose(orderSort, Comparator.<String>reverseOrder());
                break;
            case NAME_NATURAL:
            default:
                comparator = SortUtil.compose(orderSort, Comparator.<String>naturalOrder());
                break;
        }
        whitelist.sort(comparator);

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
                            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().itemRemovedFromWhitelist()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

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
    protected Mask getControlItems(GuiConfig.ControlItemsConfig section) {
        WhitelistGuiConfig.WhitelistControlItemsConfig controlConfig = (WhitelistGuiConfig.WhitelistControlItemsConfig) section;
        HybridMask mask = new HybridMask();

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.NAME_NATURAL, controlConfig.sortByName());
        putSortConfig(sortConfigMap, SortType.NAME_REVERSE, controlConfig.sortByName());
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    public enum SortType {
        NAME_NATURAL, NAME_REVERSE
    }
}
