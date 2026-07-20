package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.FilterGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.GuiUtil;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterGUI extends BaseGUI<FilterGUI.SortType, FilterGuiConfig, FilterGUI.FilterData> {

    public FilterGUI() {
        super("gui/filter.yml", FilterGuiConfig.class, SortType.class);
    }

    public void openFor(Player player) {
        FilterData data = sessions.computeIfAbsent(player.getUniqueId(), FilterData::new);
        data.confirm = false;
        SpigotInventoryUI inv = getInventory(player);
        inv.update();
        inv.open();
    }

    @Override
    protected void onBottomInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        FilterData data = sessions.get(player.getUniqueId());
        if (data == null) return;

        final ItemStack clickedItem = event.getCurrentItem();
        if ((clickedItem == null) || (clickedItem.getType() == Material.AIR)) return;

        final String validKey = ItemUtil.toMaterialKey(clickedItem);
        if (validKey.equals(Constants.INVALID)) {
            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().invalidItem()));
            return;
        }
        if (data.storage.canStore(validKey)) return;

        if (ExtraStorage.getInstance().getSetting().getNormalizedBlacklist().contains(validKey) || (ExtraStorage.getInstance().getSetting().limitWhitelist() && !ExtraStorage.getInstance().getSetting().getNormalizedWhitelist().contains(validKey))) {
            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemBlacklisted()));
            return;
        }

        Optional<Item> optional = data.storage.getItem(validKey);
        if (optional.isPresent()) optional.get().setFiltered(true);
        else data.storage.addNewItem(validKey);

        updateInventory(player.getUniqueId());
    }

    @Override
    protected void buildMask() {
        processDecorateItems();

        ButtonPaginatedMask repMask = createRepresentItemsMask(uuid -> {
            FilterData d = sessions.get(uuid);
            return d == null ? Collections.emptyList() : getRepresentItems(d, config.representItem());
        });

        FilterGuiConfig.FilterControlItemsConfig ctrl = config.controlItems();

        addAboutButton(mask, ctrl.about(),
                (uuid, text) -> {
                    FilterData d = sessions.get(uuid);
                    return applyStoragePlaceholders(d.storage, d.getPlayer().getName(), text)
                            .replaceAll(Utils.getRegex("display"), d.getPlayer().getDisplayName());
                },
                (uuid, event) -> {
                    FilterData d = sessions.get(uuid);
                    if (d.storage.getFilteredItems().isEmpty() || !event.isShiftClick()) return;
                    if (!d.confirm) {
                        d.confirm = true;
                        d.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().warn().confirmCleanup()));
                        return;
                    }
                    for (String key : d.storage.getFilteredItems().keySet()) {
                        d.storage.unfilter(key);
                    }
                    d.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().filterCleanedUp()));
                    updateInventory(uuid);
                });

        addSwitchButton(mask, ctrl.switchGui(),
                (uuid, event) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    GuiUtil.browseGUI(p, FilterGUI.this, event.isLeftClick());
                });

        addSortControls(
                sortMap -> {
                    putSortConfig(sortMap, SortType.MATERIAL, ctrl.sortByMaterial());
                    putSortConfig(sortMap, SortType.NAME, ctrl.sortByName());
                    putSortConfig(sortMap, SortType.QUANTITY, ctrl.sortByQuantity());
                },
                uuid -> sessions.get(uuid).sort, (uuid, s) -> sessions.get(uuid).sort = s,
                uuid -> sessions.get(uuid).orderSort, (uuid, b) -> sessions.get(uuid).orderSort = b
        );

        addPageNav(repMask);
    }

    private List<Button> getRepresentItems(FilterData session, Map<String, Object> section) {
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = session.storage.getFilteredItems().values().stream()
                .filter(item -> item != null && item.isLoaded());
        itemStream = sortRepresentItems(itemStream, session.sort, null, sort -> {
            switch (sort) {
                case MATERIAL:
                    return SortUtil.compose(session.orderSort, SortUtil::compareItemByMaterial, SortUtil::compareItemByQuantity);
                case NAME:
                    return SortUtil.compose(session.orderSort, SortUtil::compareItemByName, SortUtil::compareItemByQuantity);
                case QUANTITY:
                    return SortUtil.compose(session.orderSort, SortUtil::compareItemByQuantity);
                default:
                    return null;
            }
        });

        return itemStream.map(item -> {
            ItemStack iStack = displayModifier.construct(item, s -> s.replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity())));

            return (Button) (uuid, actionItem) -> {
                actionItem.setItem(iStack);
                actionItem.setAction(InventoryClickEvent.class, event -> {
                    session.storage.unfilter(item.getKey());
                    updateInventory(uuid);
                });
                return true;
            };
        }).collect(Collectors.toList());
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY
    }

    public class FilterData {
        public final UUID uuid;
        public final Storage storage;
        public SortType sort;
        public boolean orderSort = true;
        public boolean confirm;

        private FilterData(UUID uuid) {
            this.uuid = uuid;
            User user = ExtraStorage.getInstance().getUserManager().getUser(uuid);
            this.storage = user.getStorage();
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
