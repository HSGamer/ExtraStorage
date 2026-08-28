package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.SellGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.hook.economy.EconomyProvider;
import me.hsgamer.extrastorage.manager.HookManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.GuiUtil;
import me.hsgamer.extrastorage.util.SortUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SellGUI extends BaseGUI<SellGUI.SortType, SellGuiConfig, SellGUI.SellData> {
    public SellGUI(ExtraStorage plugin) {
        super(plugin, "gui/sell.yml", SellGuiConfig.class, SortType.class);
    }

    public void openFor(Player player) {
        sessions.computeIfAbsent(player.getUniqueId(), SellData::new);
        SpigotInventoryUI inv = getInventory(player);
        inv.update();
        inv.open();
    }

    @Override
    protected void buildMask() {
        processDecorateItems();

        ButtonPaginatedMask repMask = createRepresentItemsMask(uuid -> {
            SellData d = sessions.get(uuid);
            return d == null ? Collections.emptyList() : getRepresentItems(d, config.representItem());
        });

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

        addSortControls(
                sortMap -> {
                    putSortConfig(sortMap, SortType.MATERIAL, ctrl.sortByMaterial());
                    putSortConfig(sortMap, SortType.NAME, ctrl.sortByName());
                    putSortConfig(sortMap, SortType.QUANTITY, ctrl.sortByQuantity());
                    putSortConfig(sortMap, SortType.UNFILTER, ctrl.sortByUnfilter());
                },
                uuid -> sessions.get(uuid).sort, (uuid, s) -> sessions.get(uuid).sort = s,
                uuid -> sessions.get(uuid).orderSort, (uuid, b) -> sessions.get(uuid).orderSort = b
        );

        addPageNav(repMask);
    }

    private List<Button> getRepresentItems(SellData session, Map<String, Object> section) {
        EconomyProvider econ = plugin.get(HookManager.class).getEconomyProvider();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = session.getUser().getStorage().getItems().values().stream().filter(item -> item != null && item.isLoaded());
        itemStream = sortRepresentItems(itemStream, session.sort, SortType.UNFILTER, sort -> {
            switch (sort) {
                case MATERIAL: {
                    Comparator<Item> comparator = SortUtil::compareItemByMaterial;
                    comparator = comparator.thenComparing(SortUtil::compareItemByQuantity);
                    return session.orderSort ? comparator : comparator.reversed();
                }
                case NAME: {
                    Comparator<Item> comparator = SortUtil::compareItemByName;
                    comparator = comparator.thenComparing(SortUtil::compareItemByQuantity);
                    return session.orderSort ? comparator : comparator.reversed();
                }
                case QUANTITY: {
                    Comparator<Item> comparator = SortUtil::compareItemByQuantity;
                    return session.orderSort ? comparator : comparator.reversed();
                }
                default:
                    return null;
            }
        });

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
                                    .replaceAll(Utils.getRegex("status"), Utils.formatMessage(item.isFiltered() ? plugin.get(MessageConfig.class).status().filtered() : plugin.get(MessageConfig.class).status().unfiltered()))
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()))
                                    .replaceAll(Utils.getRegex("price"), price)
                                    .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(amount))
                    );

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                            if (current < 1) {
                                session.getPlayer().sendMessage(Utils.formatMessage(plugin.get(MessageConfig.class).fail().notEnoughItem()).replaceAll(Utils.getRegex("item"), plugin.get(SettingConfig.class).getNameFormatted(item.getKey(), true)));
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

                            plugin.get(HookManager.class).getEconomyProvider()
                                    .sellItem(session.getPlayer(), item.getItem(), sellAmount, rs -> {
                                        if (!rs.isSuccess()) {
                                            session.getPlayer().sendMessage(Utils.formatMessage(plugin.get(MessageConfig.class).fail().cannotBeSold()));
                                            return;
                                        }
                                        session.getUser().getStorage().subtract(item.getKey(), rs.getAmount());
                                        session.getPlayer().sendMessage(Utils.formatMessage(plugin.get(MessageConfig.class).success().itemSold())
                                                .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(rs.getAmount()))
                                                .replaceAll(Utils.getRegex("item"), plugin.get(SettingConfig.class).getNameFormatted(item.getKey(), true))
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
            return plugin.get(UserManager.class).getUser(uuid);
        }
    }
}
