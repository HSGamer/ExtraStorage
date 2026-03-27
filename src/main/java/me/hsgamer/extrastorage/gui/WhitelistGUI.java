package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.HybridMask;
import io.github.projectunified.craftux.simple.SimpleButtonMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
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

    public WhitelistGUI(Player player, GuiConfig config) {
        super(player, config, SortType.class);

        setup();
    }

    @Override
    protected SortType fallbackSort() {
        return SortType.NAME;
    }

    @Override
    protected boolean onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            final ItemStack item = event.getCurrentItem();
            if ((item == null) || (item.getType() == Material.AIR)) return false;

            final String validKey = ItemUtil.toMaterialKey(item);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return true;
            }
            Setting setting = ExtraStorage.getInstance().getSetting();
            if (setting.getBlacklist().contains(validKey)) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return true;
            }
            if (setting.getWhitelist().contains(validKey)) {
                player.sendMessage(Message.getMessage("FAIL.item-already-whitelisted"));
                return true;
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
            return true;
        }
        return super.onClick(event);
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        Setting setting = ExtraStorage.getInstance().getSetting();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        List<String> whitelist = new ArrayList<>(setting.getWhitelist());

        whitelist.sort((object1, object2) -> {
            if (orderSort) return object1.compareTo(object2);
            return object2.compareTo(object1);
        });

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

        ConfigurationSection sortByNameSection = Objects.requireNonNull(section.getConfigurationSection("SortByName"), "ControlItems.SortByName must be non-null!");
        GUIItem sortByNameItem = GUIItem.get(sortByNameSection, null);
        List<Position> sortByNameSlots = getSlots(sortByNameSection);

        SimpleButtonMask sortMask = new SimpleButtonMask();
        mask.add(sortMask);
        sortMask.setButton(sortByNameSlots, (uuid, actionItem) -> {
            sortByNameItem.apply(actionItem, user, s -> s);
            actionItem.setAction(InventoryClickEvent.class, event -> {
                orderSort = !orderSort;
                updateRepresentItems();
                update();
            });
            return true;
        });

        return mask;
    }

    public enum SortType {
        NAME
    }
}
