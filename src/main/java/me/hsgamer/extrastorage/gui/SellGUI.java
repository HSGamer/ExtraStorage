package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.SellGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.GuiUtil;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.hooks.economy.EconomyProvider;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SellGUI extends BaseGUI<SellGUI.SortType, SellGuiConfig> {

    private final Map<UUID, SellData> sessions = new HashMap<>();

    public SellGUI() {
        super("gui/sell.yml", SellGuiConfig.class, SortType.class);
    }

    @Override
    protected void clearSessions() {
        sessions.clear();
    }

    public void openFor(Player player) {
        sessions.computeIfAbsent(player.getUniqueId(), SellData::new);
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
                SellData d = sessions.get(uuid);
                if (d == null) return Collections.emptyList();
                return getRepresentItems(d, representConfig);
            }
        };
        mask.add(repMask);

        SellGuiConfig.SellControlItemsConfig ctrl = config.controlItems();

        addAboutButton(mask, ctrl.about(),
                (uuid, text) -> {
                    SellData d = sessions.get(uuid);
                    return applyStoragePlaceholders(d.getUser().getStorage(), d.getPlayer().getName(), text);
                },
                null);

        addSwitchButton(mask, ctrl.switchGui(),
                (uuid, event) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    GuiUtil.browseGUI(p, SellGUI.this, event.isLeftClick());
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
                uuid -> updateInventory(uuid));

        Map<String, Object> nextPageCfg = ctrl.nextPage();
        Map<String, Object> prevPageCfg = ctrl.previousPage();
        addPageNavMask(mask, repMask,
                GUIItem.get(nextPageCfg, null), getSlots(nextPageCfg),
                GUIItem.get(prevPageCfg, null), getSlots(prevPageCfg),
                uuid -> updateInventory(uuid));
    }

    private List<Button> getRepresentItems(SellData session, Map<String, Object> section) {
        EconomyProvider econ = ExtraStorage.getInstance().getEconomyProvider();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = session.getUser().getStorage().getItems().values().stream().filter(item -> item != null && item.isLoaded());
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
                    ItemStack sellItem = item.getItem().clone();
                    int amount = econ.getAmount(sellItem);
                    String price = econ.getPrice(session.getPlayer(), sellItem, amount);
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
                                session.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().notEnoughItem()).replaceAll(Utils.getRegex("item"), ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true)));
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
                                    .sellItem(session.getPlayer(), item.getItem(), sellAmount, rs -> {
                                        if (!rs.isSuccess()) {
                                            session.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().cannotBeSold()));
                                            return;
                                        }
                                        session.getUser().getStorage().subtract(item.getKey(), rs.getAmount());
                                        session.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().itemSold())
                                                .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(rs.getAmount()))
                                                .replaceAll(Utils.getRegex("item"), ExtraStorage.getInstance().getSetting().getNameFormatted(item.getKey(), true))
                                                .replaceAll(Utils.getRegex("price"), Digital.formatDouble("###,###.##", rs.getPrice())));
                                    });

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

    public class SellData {
        public final UUID uuid;
        public SortType sort;
        public boolean orderSort = true;

        private SellData(UUID uuid) {
            this.uuid = uuid;
            this.sort = BaseGUI.getDefaultSort(config.settings(), SortType.class);
        }

        public Player getPlayer() {
            return Bukkit.getPlayer(uuid);
        }

        public User getUser() {
            return ExtraStorage.getInstance().getUserManager().getUser(uuid);
        }
    }
}
