package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.log.Log;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageGUI extends BaseGUI<StorageGUI.SortType> {
    private final User partner;

    public StorageGUI(Player player, User partner) {
        super(player, ExtraStorage.getInstance().getStorageGuiConfig(), SortType.class);
        this.partner = ((partner == null) ? user : partner);
        this.storage = this.partner.getStorage();

        setup();
    }

    public User getPartner() {
        return partner;
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
                        switch (sort) {
                            case MATERIAL:
                                return SortUtil.compareItemByMaterial(obj1, obj2, orderSort);
                            case NAME:
                                return SortUtil.compareItemByName(obj1, obj2, orderSort);
                            case QUANTITY:
                                return SortUtil.compareItemByQuantity(obj1, obj2, orderSort);
                            case UNFILTER:
                                break;
                        }
                        return 0;
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

        addAboutButton(mask, Objects.requireNonNull(section.getConfigurationSection("About")), s -> {
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
        }, event -> {
            boolean isAdminOrSelf = (this.hasPermission(Constants.ADMIN_OPEN_PERMISSION) || partner.getUUID().equals(player.getUniqueId()));
            if ((!this.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) || (!isAdminOrSelf)) return;

            boolean status = !storage.getStatus();
            storage.setStatus(status);

            player.sendMessage(Message.getMessage("SUCCESS.storage-usage-toggled").replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (status ? "enabled" : "disabled"))));
            update();
        });

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
        MATERIAL, NAME, QUANTITY, UNFILTER
    }
}
