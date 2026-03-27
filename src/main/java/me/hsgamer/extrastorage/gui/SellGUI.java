package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.configs.Message;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;

public class SellGUI extends BaseGUI<SellGUI.SortType> {

    public SellGUI(Player player) {
        super(player, ExtraStorage.getInstance().getSellGuiConfig(), SortType.class);

        setup();
    }


    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        EconomyProvider econ = ExtraStorage.getInstance().getSetting().getEconomyProvider();
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
                .filter(item -> {
                    ItemStack sellItem = item.getItem().clone();
                    int amount = econ.getAmount(sellItem);
                    return amount >= 1 && econ.getPrice(player, sellItem, amount) != null;
                })
                .map(item -> {
                    ItemStack sellItem = item.getItem().clone();
                    int amount = econ.getAmount(sellItem);
                    String price = econ.getPrice(player, sellItem, amount);

                    ItemStack iStack = displayModifier.construct(
                            item,
                            s -> s
                                    .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (item.isFiltered() ? "filtered" : "unfiltered")))
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()))
                                    .replaceAll(Utils.getRegex("price"), price)
                                    .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(amount))
                    );

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                            if (current < 1) {
                                player.sendMessage(Message.getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true)));
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
                                    .getEconomyProvider()
                                    .sellItem(player, item.getItem(), sellAmount, rs -> {
                                        if (!rs.isSuccess()) {
                                            player.sendMessage(Message.getMessage("FAIL.cannot-be-sold"));
                                            return;
                                        }
                                        storage.subtract(item.getKey(), rs.getAmount());
                                        player.sendMessage(Message.getMessage("SUCCESS.item-sold")
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
            String UNKNOWN = Message.getMessage("STATUS.unknown");

            long space = storage.getSpace(), used = storage.getUsedSpace(), free = storage.getFreeSpace();
            double usedPercent = storage.getSpaceAsPercent(true), freePercent = storage.getSpaceAsPercent(false);

            return s
                    .replaceAll(Utils.getRegex("player"), player.getName())
                    .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (storage.getStatus() ? "enabled" : "disabled")))
                    .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                    .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                    .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                    .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                    .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
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

    private void putSortConfig(Map<SortType, SortButtonConfig<SortType>> map, SortType type, ConfigurationSection section, String key) {
        ConfigurationSection subSection = section.getConfigurationSection(key);
        if (subSection == null) return;
        map.put(type, new SortButtonConfig<>(GUIItem.get(subSection, null), getSlots(subSection)));
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, UNFILTER
    }
}
