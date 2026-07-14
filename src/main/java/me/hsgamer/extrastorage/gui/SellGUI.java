package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.config.SellGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.hooks.economy.EconomyProvider;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SellGUI extends BaseGUI<SellGUI.SortType, SellGuiConfig> {

    public SellGUI(Player player) {
        super(player, ExtraStorage.getInstance().getSellGuiConfig(), SortType.class);

        setup();
    }

    @Override
    protected List<Button> getRepresentItems(Map<String, Object> section) {
        EconomyProvider econ = ExtraStorage.getInstance().getEconomyProvider();
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
                                    .replaceAll(Utils.getRegex("status"), Utils.formatMessage(item.isFiltered() ? ExtraStorage.getInstance().getMessage().status().filtered() : ExtraStorage.getInstance().getMessage().status().unfiltered()))
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()))
                                    .replaceAll(Utils.getRegex("price"), price)
                                    .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(amount))
                    );

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                            if (current < 1) {
                                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().notEnoughItem()).replaceAll(Utils.getRegex("item"), ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true)));
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

                            ExtraStorage.getInstance().getEconomyProvider()
                                    .sellItem(player, item.getItem(), sellAmount, rs -> {
                                        if (!rs.isSuccess()) {
                                            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().cannotBeSold()));
                                            return;
                                        }
                                        storage.subtract(item.getKey(), rs.getAmount());
                                        player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().itemSold())
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
    protected Mask getControlItems(GuiConfig.ControlItemsConfig section) {
        SellGuiConfig.SellControlItemsConfig controlConfig = (SellGuiConfig.SellControlItemsConfig) section;
        HybridMask mask = new HybridMask();

        addAboutButton(mask, controlConfig.about(), s -> {
            return applyStoragePlaceholders(s, player.getName());
        }, null);

        addSwitchButton(mask, controlConfig.switchGui(), event -> {
            browseGUI(event.isLeftClick());
        });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.MATERIAL, controlConfig.sortByMaterial());
        putSortConfig(sortConfigMap, SortType.NAME, controlConfig.sortByName());
        putSortConfig(sortConfigMap, SortType.QUANTITY, controlConfig.sortByQuantity());
        putSortConfig(sortConfigMap, SortType.UNFILTER, controlConfig.sortByUnfilter());
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, UNFILTER
    }
}
