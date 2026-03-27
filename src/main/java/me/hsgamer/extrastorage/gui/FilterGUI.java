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
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterGUI extends BaseGUI<FilterGUI.SortType> {
    private boolean confirm;

    public FilterGUI(Player player, GuiConfig config) {
        super(player, config, SortType.class);
        this.confirm = false;

        setup();
    }

    @Override
    protected SortType fallbackSort() {
        return SortType.MATERIAL;
    }

    @Override
    protected boolean onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            final ItemStack clickedItem = event.getCurrentItem();
            if ((clickedItem == null) || (clickedItem.getType() == Material.AIR)) return false;

            final String validKey = ItemUtil.toMaterialKey(clickedItem);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return true;
            }
            if (storage.canStore(validKey)) return true;

            if (ExtraStorage.getInstance().getSetting().getBlacklist().contains(validKey) || (ExtraStorage.getInstance().getSetting().isLimitWhitelist() && !ExtraStorage.getInstance().getSetting().getWhitelist().contains(validKey))) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return true;
            }

            Optional<Item> optional = storage.getItem(validKey);
            if (optional.isPresent()) optional.get().setFiltered(true);
            else storage.addNewItem(validKey);

            updateRepresentItems();
            update();
            return true;
        }
        return super.onClick(event);
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = storage.getFilteredItems().values().stream()
                .filter(item -> item != null && item.isLoaded());

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
            }
            return compare;
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
                        .replaceAll(Utils.getRegex("display"), player.getDisplayName())
                        .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (storage.getStatus() ? "enabled" : "disabled")))
                        .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                        .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                        .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                        .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                        .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
            });
            actionItem.setAction(InventoryClickEvent.class, event -> {
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
            return true;
        });

        SimpleButtonMask switchMask = new SimpleButtonMask();
        mask.add(switchMask);
        switchMask.setButton(switchSlots, (uuid, actionItem) -> {
            switchGuiItem.apply(actionItem, user, s -> s);
            actionItem.setAction(InventoryClickEvent.class, event -> {
                // TODO: Implement the new switch on my own
                if (event.isLeftClick()) {
                    if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                } else if (event.isRightClick()) {
                    if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
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
                        previousSort = SortType.QUANTITY;
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
                        nextSort = SortType.MATERIAL;
                        previousSort = SortType.NAME;
                        positions = sortByQuantitySlots;
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
        MATERIAL, NAME, QUANTITY
    }
}
