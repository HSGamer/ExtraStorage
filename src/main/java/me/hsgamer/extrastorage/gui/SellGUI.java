package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.hooks.economy.EconomyProvider;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SellGUI extends BaseGUI<SellGUI.SortType> {

    public SellGUI(Player player) {
        super(player, ExtraStorage.getInstance().getSellGuiConfig(), SortType.class);

        setup();
    }


    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        EconomyProvider econ = ExtraStorage.getInstance().getSetting().resolveEconomyProvider();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = storage.getItems().values().stream().filter(item -> item != null && item.isLoaded());
        if (sort == SortType.UNFILTER) {
            itemStream = itemStream.filter(item -> !item.isFiltered());
        } else {
            itemStream = itemStream.filter(item -> item.isFiltered() || (item.getQuantity() > 0));
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
        }

        return itemStream
                .map(item -> {
                    ItemStack sellItem = item.getItem().clone();
                    int amount = econ.getAmount(sellItem);
                    String price = econ.getPrice(player, sellItem, amount);
                    return new Object[]{item, amount, price};
                })
                .filter(data -> {
                    int amount = (int) data[1];
                    String price = (String) data[2];
                    return amount >= 1 && price != null;
                })
                .map(data -> {
                    Item item = (Item) data[0];
                    int amount = (int) data[1];
                    String price = (String) data[2];

                    ItemStack iStack = displayModifier.construct(
                            item,
                            s -> s
                                    .replaceAll(Utils.getRegex("status"), ExtraStorage.getInstance().getMessage().getMessage("STATUS." + (item.isFiltered() ? "filtered" : "unfiltered")))
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()))
                                    .replaceAll(Utils.getRegex("price"), price)
                                    .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(amount))
                    );

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                            if (current < 1) {
                                player.sendMessage(ExtraStorage.getInstance().getMessage().getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true)));
                                return;
                            }

                            int sellAmount;
                            if (event.isShiftClick())
                                sellAmount = Digital.getBetween(1, Integer.MAX_VALUE, current);
                            else if (event.isLeftClick())
                                sellAmount = amount;
                            else if (event.isRightClick())
                                sellAmount = Digital.getBetween(1, current, iStack.getMaxStackSize());
                            else return;

                            ExtraStorage.getInstance().getSetting()
                                    .resolveEconomyProvider()
                                    .sellItem(player, item.getItem(), sellAmount, rs -> {
                                        if (!rs.isSuccess()) {
                                            player.sendMessage(ExtraStorage.getInstance().getMessage().getMessage("FAIL.cannot-be-sold"));
                                            return;
                                        }
                                        storage.subtract(item.getKey(), rs.getAmount());
                                        player.sendMessage(ExtraStorage.getInstance().getMessage().getMessage("SUCCESS.item-sold")
                                                .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(rs.getAmount()))
                                                .replaceAll(Utils.getRegex("item"), ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true))
                                                .replaceAll(Utils.getRegex("price"), Digital.formatDouble("###,###.##", rs.getPrice())));
                                    });

                            updateRepresentItems();
                            update();
                        });
                        return true;
                    };
                })
                .collect(Collectors.toList());
    }

    @Override
    protected Mask getControlItems(ConfigurationSection section) {
        HybridMask mask = new HybridMask();

        addAboutButton(mask, Objects.requireNonNull(section.getConfigurationSection("About")), s -> {
            return applyStoragePlaceholders(s, player.getName());
        }, null);

        addSwitchButton(mask, Objects.requireNonNull(section.getConfigurationSection("SwitchGui")), event -> {
            browseGUI(event.isLeftClick());
        });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.MATERIAL, section, "SortByMaterial");
        putSortConfig(sortConfigMap, SortType.NAME, section, "SortByName");
        putSortConfig(sortConfigMap, SortType.QUANTITY, section, "SortByQuantity");
        putSortConfig(sortConfigMap, SortType.UNFILTER, section, "SortByUnfilter");
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, UNFILTER
    }
}
