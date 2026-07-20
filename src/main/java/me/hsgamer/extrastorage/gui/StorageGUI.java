package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.SettingConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.StorageGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.GuiUtil;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageGUI extends BaseGUI<StorageGUI.SortType, StorageGuiConfig> {

    final Map<UUID, StorageData> sessions = new HashMap<>();

    public StorageGUI() {
        super("gui/storage.yml", StorageGuiConfig.class, SortType.class);
    }

    @Override
    protected void clearSessions() {
        sessions.clear();
    }

    public StorageData getSessionData(UUID uuid) {
        return sessions.get(uuid);
    }

    public void openFor(Player player, User partner) {
        StorageData data = sessions.computeIfAbsent(player.getUniqueId(), k -> new StorageData(k));
        data.setPartner(partner);
        SpigotInventoryUI inv = getInventory(player);
        inv.update();
        inv.open();
    }

    @Override
    protected void buildMask() {
        Map<String, Map<String, Object>> decorateItems = config.decorateItems();
        if (decorateItems != null) {
            for (Map<String, Object> itemConfig : decorateItems.values()) {
                processDecorateItem(mask, itemConfig);
            }
        }

        Map<String, Object> representConfig = config.representItem();
        List<Position> representSlots = getSlots(representConfig);
        ButtonPaginatedMask repMask = new ButtonPaginatedMask(u -> representSlots) {
            @Override
            public @NotNull List<Button> getButtons(@NotNull UUID uuid) {
                StorageData s = sessions.get(uuid);
                if (s == null) return Collections.emptyList();
                return getRepresentItems(s, representConfig);
            }
        };
        mask.add(repMask);

        StorageGuiConfig.StorageControlItemsConfig ctrl = config.controlItems();

        addAboutButton(mask, ctrl.about(),
                (uuid, text) -> {
                    StorageData s = sessions.get(uuid);
                    return applyStoragePlaceholders(s.storage, s.partner.getName(), text);
                },
                (uuid, event) -> {
                    StorageData s = sessions.get(uuid);
                    Player p = s.getPlayer();
                    boolean isAdminOrSelf = (p.isOp() || p.hasPermission(Constants.ADMIN_OPEN_PERMISSION) || s.partner.getUUID().equals(uuid));
                    if (!p.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION) || !isAdminOrSelf) return;
                    boolean status = !s.storage.getStatus();
                    s.storage.setStatus(status);
                    p.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().storageUsageToggled())
                            .replaceAll(Utils.getRegex("status"), Utils.formatMessage(status ? ExtraStorage.getInstance().getMessage().status().enabled() : ExtraStorage.getInstance().getMessage().status().disabled())));
                    updateInventory(uuid);
                });

        addSwitchButton(mask, ctrl.switchGui(),
                (uuid, event) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    GuiUtil.browseGUI(p, StorageGUI.this, event.isLeftClick());
                });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.MATERIAL, ctrl.sortByMaterial());
        putSortConfig(sortConfigMap, SortType.NAME, ctrl.sortByName());
        putSortConfig(sortConfigMap, SortType.QUANTITY, ctrl.sortByQuantity());
        putSortConfig(sortConfigMap, SortType.UNFILTER, ctrl.sortByUnfilter());
        addSortMask(mask, sortConfigMap,
                uuid -> sessions.get(uuid).sort,
                (uuid, s) -> sessions.get(uuid).sort = s,
                uuid -> sessions.get(uuid).orderSort,
                (uuid, b) -> sessions.get(uuid).orderSort = b,
                this::updateInventory);

        Map<String, Object> nextPageCfg = ctrl.nextPage();
        Map<String, Object> prevPageCfg = ctrl.previousPage();
        addPageNavMask(mask, repMask,
                GUIItem.get(nextPageCfg, null), getSlots(nextPageCfg),
                GUIItem.get(prevPageCfg, null), getSlots(prevPageCfg),
                this::updateInventory);
    }

    private List<Button> getRepresentItems(StorageData session, Map<String, Object> section) {
        SettingConfig setting = ExtraStorage.getInstance().getSetting();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = session.storage.getItems().values().stream().filter(item -> item != null && item.isLoaded());
        if (session.sort == SortType.UNFILTER) {
            itemStream = itemStream.filter(item -> !item.isFiltered());
        } else {
            itemStream = itemStream.filter(item -> item.isFiltered() || (item.getQuantity() > 0));
            Comparator<Item> comparator = null;
            switch (session.sort) {
                case MATERIAL:
                    comparator = SortUtil.compose(session.orderSort, SortUtil::compareItemByMaterial, SortUtil::compareItemByQuantity);
                    break;
                case NAME:
                    comparator = SortUtil.compose(session.orderSort, SortUtil::compareItemByName, SortUtil::compareItemByQuantity);
                    break;
                case QUANTITY:
                    comparator = SortUtil.compose(session.orderSort, SortUtil::compareItemByQuantity);
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
                            Player player = session.getPlayer();
                            ItemStack clicked = event.getCurrentItem();
                            if ((clicked == null) || (clicked.getType() == Material.AIR)) return;

                            final ClickType click = event.getClick();
                            if (click == ClickType.SHIFT_RIGHT) {
                                if (session.storage.isMaxSpace()) {
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

                                    Optional<Item> optional = session.storage.getItem(is);
                                    if (!optional.isPresent()) continue;
                                    Item i = optional.get();
                                    if (!i.isLoaded()) continue;

                                    if (item.getType() != i.getType()) continue;
                                    if (!key.equalsIgnoreCase(ItemUtil.toMaterialKey(is))) continue;

                                    int amount = is.getAmount();
                                    long freeSpace = session.storage.getFreeSpace();
                                    if ((freeSpace == -1) || ((freeSpace - amount) >= 0)) {
                                        count += amount;
                                        session.storage.add(item.getKey(), amount);
                                        player.getInventory().removeItem(is);
                                        continue;
                                    }
                                    amount = (int) freeSpace;
                                    count += amount;
                                    session.storage.add(key, amount);

                                    if (is.getAmount() > amount) is.setAmount(is.getAmount() - amount);
                                    else player.getInventory().removeItem(is);
                                    break;
                                }
                                if (count == 0) {
                                    player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().notEnoughItemInInventory()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                    return;
                                }

                                if (setting.log().transfer()) {
                                    ExtraStorage.getInstance().getLog().log(player, session.partner.getOfflinePlayer(), Log.Action.TRANSFER, key, count, -1);
                                }

                                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().movedItemsToStorage())
                                        .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(count))
                                        .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                if (!session.partner.isOnline()) session.partner.save();

                                updateInventory(uuid);
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
                            else if (event.isLeftClick()) ;
                            else if (event.isRightClick())
                                vanillaItem.setAmount(Math.min(current, clicked.getMaxStackSize()));
                            else return;
                            if (item.getType() == ItemUtil.ItemType.VANILLA) {
                                vanillaItem.setItemMeta(null);
                            }

                            int free = ItemUtil.getFreeSpace(player, vanillaItem);
                            if (free == -1) {
                                player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().inventoryIsFull()));
                                return;
                            }
                            vanillaItem.setAmount(free);

                            ItemUtil.giveItem(player, vanillaItem);
                            session.storage.subtract(item.getKey(), free);

                            if (setting.log().withdraw()) {
                                ExtraStorage.getInstance().getLog().log(player, session.partner.getOfflinePlayer(), Log.Action.WITHDRAW, item.getKey(), free, -1);
                            }

                            if (!session.partner.isOnline()) session.partner.save();

                            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().withdrewItem())
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(free))
                                    .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

                            updateInventory(uuid);
                        });
                        return true;
                    };
                })
                .collect(Collectors.toList());
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, UNFILTER
    }

    public class StorageData {
        public final UUID uuid;
        public User partner;
        public Storage storage;
        public SortType sort;
        public boolean orderSort = true;

        private StorageData(UUID uuid) {
            this.uuid = uuid;
            this.sort = BaseGUI.getDefaultSort(config.settings(), SortType.class);
        }

        public Player getPlayer() {
            return Bukkit.getPlayer(uuid);
        }

        public User getUser() {
            return ExtraStorage.getInstance().getUserManager().getUser(uuid);
        }

        void setPartner(User partner) {
            if (partner == null) partner = getUser();
            this.partner = partner;
            this.storage = partner.getStorage();
        }
    }
}
