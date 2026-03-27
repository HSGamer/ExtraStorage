package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterGUI extends BaseGUI<FilterGUI.SortType> {
    private boolean confirm;

    public FilterGUI(Player player) {
        super(player, ExtraStorage.getInstance().getFilterGuiConfig(), SortType.class);
        this.confirm = false;

        setup();
    }


    @Override
    protected boolean onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getWhoClicked().getOpenInventory().getBottomInventory()) {
            final ItemStack clickedItem = event.getCurrentItem();
            if ((clickedItem == null) || (clickedItem.getType() == Material.AIR)) return false;

            final String validKey = ItemUtil.toMaterialKey(clickedItem);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return false;
            }
            if (storage.canStore(validKey)) return false;

            if (ExtraStorage.getInstance().getSetting().getBlacklist().contains(validKey) || (ExtraStorage.getInstance().getSetting().isLimitWhitelist() && !ExtraStorage.getInstance().getSetting().getWhitelist().contains(validKey))) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return false;
            }

            Optional<Item> optional = storage.getItem(validKey);
            if (optional.isPresent()) optional.get().setFiltered(true);
            else storage.addNewItem(validKey);

            updateRepresentItems();
            update();
        }
        return super.onClick(event);
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = storage.getFilteredItems().values().stream()
                .filter(item -> item != null && item.isLoaded());

        itemStream = itemStream.sorted((obj1, obj2) -> {
            switch (sort) {
                case MATERIAL:
                    return SortUtil.compareItemByMaterial(obj1, obj2, orderSort);
                case NAME:
                    return SortUtil.compareItemByName(obj1, obj2, orderSort);
                case QUANTITY:
                    return SortUtil.compareItemByQuantity(obj1, obj2, orderSort);
                default:
                    return 0;
            }
        });

        return itemStream.map(item -> {
            String key = item.getKey();
            ItemStack iStack = displayModifier.construct(item, s -> s.replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity())));

            return (Button) (uuid, actionItem) -> {
                actionItem.setItem(iStack);
                actionItem.setAction(InventoryClickEvent.class, event -> {
                    storage.unfilter(key);
                    updateRepresentItems();
                    update();
                });
                return true;
            };
        }).collect(Collectors.toList());
    }

    @Override
    protected Mask getControlItems(ConfigurationSection section) {
        HybridMask mask = new HybridMask();

        addAboutButton(mask, Objects.requireNonNull(section.getConfigurationSection("About")), s -> {
            String UNKNOWN = Message.getMessage("STATUS.unknown");
            long space = storage.getSpace(), used = storage.getUsedSpace(), free = storage.getFreeSpace();
            double usedPercent = storage.getSpaceAsPercent(true), freePercent = storage.getSpaceAsPercent(false);

            return s
                    .replaceAll(Utils.getRegex("player"), player.getName())
                    .replaceAll(Utils.getRegex("display"), player.getDisplayName())
                    .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (storage.getStatus() ? "enabled" : "disabled")))
                    .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                    .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                    .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                    .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                    .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
        }, event -> {
            if (storage.getFilteredItems().isEmpty() || (!event.isShiftClick())) return;

            if (!confirm) {
                confirm = true;
                player.sendMessage(Message.getMessage("WARN.confirm-cleanup"));
                return;
            }

            for (String key : storage.getFilteredItems().keySet()) storage.unfilter(key);
            player.sendMessage(Message.getMessage("SUCCESS.filter-cleaned-up"));

            updateRepresentItems();
            update();
        });

        addSwitchButton(mask, Objects.requireNonNull(section.getConfigurationSection("SwitchGui")), event -> {
            browseGUI(event.isLeftClick());
        });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.MATERIAL, section, "SortByMaterial");
        putSortConfig(sortConfigMap, SortType.NAME, section, "SortByName");
        putSortConfig(sortConfigMap, SortType.QUANTITY, section, "SortByQuantity");
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    private void putSortConfig(Map<SortType, SortButtonConfig<SortType>> map, SortType type, ConfigurationSection section, String key) {
        ConfigurationSection subSection = section.getConfigurationSection(key);
        if (subSection == null) return;
        map.put(type, new SortButtonConfig<>(GUIItem.get(subSection, null), getSlots(subSection)));
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY
    }
}
