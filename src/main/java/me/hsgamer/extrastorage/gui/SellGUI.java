package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.ActionItem;
import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.HybridMask;
import io.github.projectunified.craftux.simple.SimpleButtonMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.hooks.economy.EconomyProvider;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SellGUI extends BaseGUI<SellGUI.SortType> {

    public SellGUI(Player player, GuiConfig config) {
        super(player, config, SortType.class);

        setup();
    }

    @Override
    protected SortType fallbackSort() {
        return SortType.MATERIAL;
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        EconomyProvider econ = ExtraStorage.getInstance().getSetting().getEconomyProvider();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = storage.getItems().values().stream()
                .filter(item -> item != null && item.isLoaded());

        if (sort != SortType.UNFILTER) {
            itemStream = itemStream.sorted((obj1, obj2) -> {
                int compare = 0;
                switch (sort) {
                    case MATERIAL:
                        if (orderSort) {
                            compare = obj1.getKey().compareTo(obj2.getKey());
                            if (compare == 0)
                                compare = Integer.compare((int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE));
                        } else {
                            compare = obj2.getKey().compareTo(obj1.getKey());
                            if (compare == 0)
                                compare = Integer.compare((int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE));
                        }
                        break;
                    case NAME:
                        String name1 = ExtraStorage.getInstance().getSetting().getNameFormatted(obj1.getKey(), false), name2 = ExtraStorage.getInstance().getSetting().getNameFormatted(obj2.getKey(), false);
                        if (orderSort) {
                            compare = name1.compareTo(name2);
                            if (compare == 0)
                                compare = Integer.compare((int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE));
                        } else {
                            compare = name2.compareTo(name1);
                            if (compare == 0)
                                compare = Integer.compare((int) Math.min(obj1.getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj2.getQuantity(), Integer.MAX_VALUE));
                        }
                        break;
                    case QUANTITY:
                        if (orderSort) {
                            compare = Long.compare(obj2.getQuantity(), obj1.getQuantity());
                            if (compare == 0) compare = obj1.getKey().compareTo(obj2.getKey());
                        } else {
                            compare = Long.compare(obj1.getQuantity(), obj2.getQuantity());
                            if (compare == 0) compare = obj2.getKey().compareTo(obj1.getKey());
                        }
                        break;
                    case UNFILTER:
                        break;
                }
                return compare;
            });
        } else {
            itemStream = itemStream.filter(item -> !item.isFiltered());
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

        ConfigurationSection aboutItemSection = Objects.requireNonNull(section.getConfigurationSection("About"), "ControlItems.About must be non-null!");
        GUIItem aboutItem = GUIItem.get(aboutItemSection, null);
        List<Position> aboutItemSlots = getSlots(aboutItemSection);

        ConfigurationSection sortByMaterialSection = Objects.requireNonNull(section.getConfigurationSection("SortByMaterial"), "ControlItems.SortByMaterial must be non-null!");
        GUIItem sortByMaterialItem = GUIItem.get(sortByMaterialSection, null);
        List<Position> sortByMaterialSlots = getSlots(sortByMaterialSection);

        ConfigurationSection sortByNameSection = Objects.requireNonNull(section.getConfigurationSection("SortByName"), "ControlItems.SortByName must be non-null!");
        GUIItem sortByNameItem = GUIItem.get(sortByNameSection, null);
        List<Position> sortByNameSlots = getSlots(sortByNameSection);

        ConfigurationSection sortByQuantitySection = Objects.requireNonNull(section.getConfigurationSection("SortByQuantity"), "ControlItems.SortByQuantity must be non-null!");
        GUIItem sortByQuantityItem = GUIItem.get(sortByQuantitySection, null);
        List<Position> sortByQuantitySlots = getSlots(sortByQuantitySection);

        ConfigurationSection sortByUnfilterSection = Objects.requireNonNull(section.getConfigurationSection("SortByUnfilter"), "ControlItems.SortByUnfilter must be non-null!");
        GUIItem sortByUnfilterItem = GUIItem.get(sortByUnfilterSection, null);
        List<Position> sortByUnfilterSlots = getSlots(sortByUnfilterSection);

        ConfigurationSection switchSection = Objects.requireNonNull(section.getConfigurationSection("SwitchGui"), "ControlItems.SwitchGui must be non-null!");
        GUIItem switchGuiItem = GUIItem.get(switchSection, null);
        List<Position> switchSlots = getSlots(switchSection);

        SimpleButtonMask aboutMask = new SimpleButtonMask();
        mask.add(aboutMask);
        aboutMask.setButton(aboutItemSlots, (uuid, actionItem) -> {
            aboutItem.apply(actionItem, user, s -> {
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
            });
            return true;
        });

        SimpleButtonMask switchMask = new SimpleButtonMask();
        mask.add(switchMask);
        switchMask.setButton(switchSlots, (uuid, actionItem) -> {
            switchGuiItem.apply(actionItem, user, s -> s);
            actionItem.setAction(InventoryClickEvent.class, event -> {
                // TODO: Implement the new switch on my own
                if (event.isLeftClick()) {
                    if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                } else if (event.isRightClick()) {
                    if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                }
            });
            return true;
        });

        Mask sortMask = new Mask() {
            @Override
            public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                Map<Position, Consumer<ActionItem>> map = new HashMap<>();

                GUIItem displayItem = null;
                List<Position> positions = new ArrayList<>();
                SortType nextSort = null;
                SortType previousSort = null;
                switch (sort) {
                    case MATERIAL: {
                        displayItem = sortByMaterialItem;
                        nextSort = SortType.NAME;
                        previousSort = SortType.UNFILTER;
                        positions = sortByMaterialSlots;
                        break;
                    }
                    case NAME: {
                        displayItem = sortByNameItem;
                        nextSort = SortType.QUANTITY;
                        previousSort = SortType.MATERIAL;
                        positions = sortByNameSlots;
                        break;
                    }
                    case QUANTITY: {
                        displayItem = sortByQuantityItem;
                        nextSort = SortType.UNFILTER;
                        previousSort = SortType.NAME;
                        positions = sortByQuantitySlots;
                        break;
                    }
                    case UNFILTER: {
                        displayItem = sortByUnfilterItem;
                        nextSort = SortType.MATERIAL;
                        previousSort = SortType.QUANTITY;
                        positions = sortByUnfilterSlots;
                        break;
                    }
                }

                if (displayItem != null && !positions.isEmpty()) {
                    GUIItem finalDisplayItem = displayItem;
                    SortType finalNextSort = nextSort;
                    SortType finalPreviousSort = previousSort;
                    Consumer<ActionItem> actionItemConsumer = actionItem -> {
                        finalDisplayItem.apply(actionItem, user, s -> s);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            if (event.isShiftClick()) {
                                orderSort = !orderSort;
                            } else {
                                SortType newSort = event.isLeftClick() ? finalNextSort : (event.isRightClick() ? finalPreviousSort : null);
                                if (newSort == null) return;
                                sort = newSort;
                            }
                            updateRepresentItems();
                            update();
                        });
                    };
                    positions.forEach(position -> map.put(position, actionItemConsumer));
                }
                return map;
            }
        };
        mask.add(sortMask);

        return mask;
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, UNFILTER
    }
}
