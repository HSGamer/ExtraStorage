package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.FilterGuiConfig;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterGUI extends BaseGUI<FilterGUI.SortType, FilterGuiConfig> {
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
                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().invalidItem()));
                return false;
            }
            if (storage.canStore(validKey)) return false;

            if (ExtraStorage.getInstance().getSetting().getNormalizedBlacklist().contains(validKey) || (ExtraStorage.getInstance().getSetting().limitWhitelist() && !ExtraStorage.getInstance().getSetting().getNormalizedWhitelist().contains(validKey))) {
                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemBlacklisted()));
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
    protected List<Button> getRepresentItems(Map<String, Object> section) {
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = storage.getFilteredItems().values().stream()
                .filter(item -> item != null && item.isLoaded());

        Comparator<Item> comparator = null;
        switch (sort) {
            case MATERIAL:
                comparator = SortUtil.compose(orderSort, SortUtil::compareItemByMaterial, SortUtil::compareItemByQuantity);
                break;
            case NAME:
                comparator = SortUtil.compose(orderSort, SortUtil::compareItemByName, SortUtil::compareItemByQuantity);
                break;
            case QUANTITY:
                comparator = SortUtil.compose(orderSort, SortUtil::compareItemByQuantity);
                break;
            default:
                break;
        }
        if (comparator != null) {
            itemStream = itemStream.sorted(comparator);
        }

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
    protected Mask getControlItems(GuiConfig.ControlItemsConfig section) {
        FilterGuiConfig.FilterControlItemsConfig controlConfig = (FilterGuiConfig.FilterControlItemsConfig) section;
        HybridMask mask = new HybridMask();

        addAboutButton(mask, controlConfig.about(), s -> {
            return applyStoragePlaceholders(s, player.getName())
                    .replaceAll(Utils.getRegex("display"), player.getDisplayName());
        }, event -> {
            if (storage.getFilteredItems().isEmpty() || (!event.isShiftClick())) return;

            if (!confirm) {
                confirm = true;
                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().warn().confirmCleanup()));
                return;
            }

            for (String key : storage.getFilteredItems().keySet()) storage.unfilter(key);
            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().filterCleanedUp()));

            updateRepresentItems();
            update();
        });

        addSwitchButton(mask, controlConfig.switchGui(), event -> {
            browseGUI(event.isLeftClick());
        });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.MATERIAL, controlConfig.sortByMaterial());
        putSortConfig(sortConfigMap, SortType.NAME, controlConfig.sortByName());
        putSortConfig(sortConfigMap, SortType.QUANTITY, controlConfig.sortByQuantity());
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY
    }
}
