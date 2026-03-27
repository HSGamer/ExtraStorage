package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.ActionItem;
import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.HybridMask;
import io.github.projectunified.craftux.simple.SimpleButtonMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageGUI extends BaseGUI<StorageGUI.SortType> {
    private final User partner;

    public StorageGUI(Player player, GuiConfig config, User partner) {
        super(player, config, SortType.class);
        this.partner = ((partner == null) ? user : partner);
        this.storage = this.partner.getStorage();

        setup();
    }

    @Override
    protected SortType fallbackSort() {
        return SortType.MATERIAL;
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        Setting setting = ExtraStorage.getInstance().getSetting();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = storage.getItems().values().stream()
                .filter(item -> item != null && item.isLoaded());
        if (sort != SortType.UNFILTER) {
            itemStream = itemStream
                    .filter(item -> item.isFiltered() || (item.getQuantity() > 0))
                    .sorted((obj1, obj2) -> {
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
                                String name1 = setting.getNameFormatted(obj1.getKey(), false), name2 = setting.getNameFormatted(obj2.getKey(), false);
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
                            case TIME:
                            case UNFILTER:
                                break;
                        }
                        return compare;
                    });
        } else {
            itemStream = itemStream.filter(item -> !item.isFiltered());
        }
        return itemStream
                .map(item -> {
                    String key = item.getKey();
                    ItemStack iStack = displayModifier.construct(
                            item,
                            s -> s
                                    .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (item.isFiltered() ? "filtered" : "unfiltered")))
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()))
                    );

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            ItemStack clicked = event.getCurrentItem();
                            if ((clicked == null) || (clicked.getType() == Material.AIR)) return;

                            final ClickType click = event.getClick();
                            if (click == ClickType.SHIFT_RIGHT) {
                                // Chuyển tất cả vật phẩm trong kho đồ của người chơi vào kho chứa:
                                if (storage.isMaxSpace()) {
                                    player.sendMessage(Message.getMessage("FAIL.storage-is-full"));
                                    return;
                                }
                                if (!item.isFiltered()) {
                                    player.sendMessage(Message.getMessage("FAIL.item-not-filtered"));
                                    return;
                                }
                                if (setting.getBlacklist().contains(key) || (setting.isLimitWhitelist() && !setting.getWhitelist().contains(key))) {
                                    player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                                    return;
                                }

                                int count = 0;
                                ItemStack[] items = player.getInventory().getStorageContents();
                                for (ItemStack is : items) {
                                    if ((is == null) || (is.getType() == Material.AIR)) continue;

                                    Optional<Item> optional = storage.getItem(is);
                                    if (!optional.isPresent()) continue;
                                    Item i = optional.get();
                                    if (!i.isLoaded()) continue;

                                    if (item.getType() != i.getType()) continue;
                                    if (!key.equalsIgnoreCase(ItemUtil.toMaterialKey(is))) continue;

                                    int amount = is.getAmount();
                                    long freeSpace = storage.getFreeSpace();
                                    if ((freeSpace == -1) || ((freeSpace - amount) >= 0)) {
                                        count += amount;
                                        storage.add(item.getKey(), amount);
                                        player.getInventory().removeItem(is);
                                        continue;
                                    }
                                    amount = (int) freeSpace;
                                    count += amount;
                                    storage.add(key, amount);

                                    if (is.getAmount() > amount) is.setAmount(is.getAmount() - amount);
                                    else player.getInventory().removeItem(is);
                                    break;
                                }
                                if (count == 0) {
                                    player.sendMessage(Message.getMessage("FAIL.not-enough-item-in-inventory").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                    return;
                                }

                                if (setting.isLogTransfer()) {
                                    ExtraStorage.getInstance().getLog().log(player, partner.getOfflinePlayer(), Log.Action.TRANSFER, key, count, -1);
                                }

                                player.sendMessage(Message.getMessage("SUCCESS.moved-items-to-storage")
                                        .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(count))
                                        .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                if (!partner.isOnline()) partner.save();

                                updateRepresentItems();
                                update();
                                return;
                            }

                            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                            if (current <= 0) {
                                player.sendMessage(Message.getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                return;
                            }

                            ItemStack vanillaItem = item.getItem();
                            if (click == ClickType.SHIFT_LEFT)
                                vanillaItem.setAmount(current);
                            else if (event.isLeftClick()) ; // Bỏ qua vì phần này chỉ rút 1 vật phẩm.
                            else if (event.isRightClick())
                                vanillaItem.setAmount(Math.min(current, clicked.getMaxStackSize()));
                            else return;
                            if (item.getType() == ItemUtil.ItemType.VANILLA) {
                                /*
                                 * Cần xoá Meta của Item khi rút vì xảy ra trường hợp sau khi rút xong
                                 * thì item sẽ có Meta, khiến cho việc drop ra mặt đất và không thể
                                 * nhặt lại vào kho chứa được.
                                 * Việc setItemMeta(null) sẽ không bị lỗi ở bất kỳ phiên bản nào.
                                 */
                                vanillaItem.setItemMeta(null);
                            }

                            int free = getFreeSpace(vanillaItem);
                            if (free == -1) {
                                // Nếu kho đồ đã đầy:
                                player.sendMessage(Message.getMessage("FAIL.inventory-is-full"));
                                return;
                            }
                            vanillaItem.setAmount(free);

                            ItemUtil.giveItem(player, vanillaItem);
                            storage.subtract(item.getKey(), free);

                            if (setting.isLogWithdraw()) {
                                ExtraStorage.getInstance().getLog().log(player, partner.getOfflinePlayer(), Log.Action.WITHDRAW, item.getKey(), free, -1);
                            }

                            if (!partner.isOnline()) partner.save();

                            player.sendMessage(Message.getMessage("SUCCESS.withdrew-item")
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(free))
                                    .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

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
                        .replaceAll(Utils.getRegex("player"), partner.getName())
                        .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (storage.getStatus() ? "enabled" : "disabled")))
                        .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                        .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                        .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                        .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                        .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
            });
            actionItem.setAction(InventoryClickEvent.class, event -> {
                boolean isAdminOrSelf = (this.hasPermission(Constants.ADMIN_OPEN_PERMISSION) || partner.getUUID().equals(player.getUniqueId()));
                if ((!this.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) || (!isAdminOrSelf)) return;

                boolean status = !storage.getStatus();
                storage.setStatus(status);

                player.sendMessage(Message.getMessage("SUCCESS.storage-usage-toggled").replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (status ? "enabled" : "disabled"))));
                update();
            });
            return true;
        });

        SimpleButtonMask switchMask = new SimpleButtonMask();
        mask.add(switchMask);
        switchMask.setButton(switchSlots, (uuid, actionItem) -> {
            switchGuiItem.apply(actionItem, partner, s -> s);
            actionItem.setAction(InventoryClickEvent.class, event -> {
                if (event.isLeftClick()) {
                    if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                } else if (event.isRightClick()) {
                    if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
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
                        previousSort = SortType.MATERIAL;
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
                    case TIME: {
                        break;
                    }
                }

                if (displayItem != null && !positions.isEmpty()) {
                    GUIItem finalDisplayItem = displayItem;
                    SortType finalNextSort = nextSort;
                    SortType finalPreviousSort = previousSort;
                    Consumer<ActionItem> actionItemConsumer = actionItem -> {
                        finalDisplayItem.apply(actionItem, partner, s -> s);
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

    /*
     * Trả về khoảng trống còn lại trong kho đồ của người chơi:
     * Sẽ là -1 nếu không còn khoảng trống nào.
     */
    private int getFreeSpace(ItemStack item) {
        ItemStack[] items = player.getInventory().getStorageContents();
        int empty = 0;
        for (ItemStack stack : items) {
            if ((stack == null) || (stack.getType() == Material.AIR)) {
                empty += item.getMaxStackSize();
                continue;
            }
            if (!item.isSimilar(stack)) continue;
            empty += (stack.getMaxStackSize() - stack.getAmount());
        }
        if (empty > 0) return Math.min(empty, item.getAmount());
        return -1;
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, TIME, UNFILTER
    }
}
