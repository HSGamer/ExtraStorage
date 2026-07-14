package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.SettingConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.config.StorageGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageGUI extends BaseGUI<StorageGUI.SortType, StorageGuiConfig> {
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
    protected List<Button> getRepresentItems(Map<String, Object> section) {
        SettingConfig setting = ExtraStorage.getInstance().getSetting();
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
                    String key = item.getKey();
                    ItemStack iStack = displayModifier.construct(
                            item,
                            s -> s
                                    .replaceAll(Utils.getRegex("status"), Utils.formatMessage(item.isFiltered() ? ExtraStorage.getInstance().getMessage().status().filtered() : ExtraStorage.getInstance().getMessage().status().unfiltered()))
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
                                    player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().storageIsFull()));
                                    return;
                                }
                                if (!item.isFiltered()) {
                                    player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemNotFiltered()));
                                    return;
                                }
                                if (setting.getNormalizedBlacklist().contains(key) || (setting.limitWhitelist() && !setting.getNormalizedWhitelist().contains(key))) {
                                    player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemBlacklisted()));
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
                                    player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().notEnoughItemInInventory()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                    return;
                                }

                                if (setting.log().transfer()) {
                                    ExtraStorage.getInstance().getLog().log(player, partner.getOfflinePlayer(), Log.Action.TRANSFER, key, count, -1);
                                }

                                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().movedItemsToStorage())
                                        .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(count))
                                        .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                if (!partner.isOnline()) partner.save();

                                updateRepresentItems();
                                update();
                                return;
                            }

                            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                            if (current <= 0) {
                                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().notEnoughItem()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
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

                            int free = ItemUtil.getFreeSpace(player, vanillaItem);
                            if (free == -1) {
                                // Nếu kho đồ đã đầy:
                                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().inventoryIsFull()));
                                return;
                            }
                            vanillaItem.setAmount(free);

                            ItemUtil.giveItem(player, vanillaItem);
                            storage.subtract(item.getKey(), free);

                            if (setting.log().withdraw()) {
                                ExtraStorage.getInstance().getLog().log(player, partner.getOfflinePlayer(), Log.Action.WITHDRAW, item.getKey(), free, -1);
                            }

                            if (!partner.isOnline()) partner.save();

                            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().withdrewItem())
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
    protected Mask getControlItems(GuiConfig.ControlItemsConfig section) {
        StorageGuiConfig.StorageControlItemsConfig controlConfig = (StorageGuiConfig.StorageControlItemsConfig) section;
        HybridMask mask = new HybridMask();

        addAboutButton(mask, controlConfig.about(), s -> {
            return applyStoragePlaceholders(s, partner.getName());
        }, event -> {
            boolean isAdminOrSelf = (this.hasPermission(Constants.ADMIN_OPEN_PERMISSION) || partner.getUUID().equals(player.getUniqueId()));
            if ((!this.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) || (!isAdminOrSelf)) return;

            boolean status = !storage.getStatus();
            storage.setStatus(status);

            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().storageUsageToggled()).replaceAll(Utils.getRegex("status"), Utils.formatMessage(status ? ExtraStorage.getInstance().getMessage().status().enabled() : ExtraStorage.getInstance().getMessage().status().disabled())));
            update();
        });

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
