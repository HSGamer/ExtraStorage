package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.FilterGuiConfig;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterGUI extends BaseGUI<FilterGUI.SortType, FilterGuiConfig> {

    private final Map<UUID, FilterData> sessions = new HashMap<>();

    public FilterGUI() {
        super("gui/filter.yml", FilterGuiConfig.class, SortType.class);
    }

    @Override
    protected void clearSessions() {
        sessions.clear();
    }

    public void openFor(Player player) {
        FilterData data = sessions.computeIfAbsent(player.getUniqueId(), k -> {
            User user = ExtraStorage.getInstance().getUserManager().getUser(player);
            return new FilterData(player, user);
        });
        data.confirm = false;
        getInventory(player).open();
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
                FilterData d = sessions.get(uuid);
                if (d == null) return Collections.emptyList();
                return getRepresentItems(d, representConfig);
            }
        };
        mask.add(repMask);

        FilterGuiConfig.FilterControlItemsConfig ctrl = config.controlItems();

        addAboutButton(mask, ctrl.about(),
                (uuid, text) -> {
                    FilterData d = sessions.get(uuid);
                    return applyStoragePlaceholders(d.storage, d.player.getName(), text)
                            .replaceAll(Utils.getRegex("display"), d.player.getDisplayName());
                },
                (uuid, event) -> {
                    FilterData d = sessions.get(uuid);
                    if (d.storage.getFilteredItems().isEmpty() || !event.isShiftClick()) return;
                    if (!d.confirm) {
                        d.confirm = true;
                        d.player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().warn().confirmCleanup()));
                        return;
                    }
                    for (String key : d.storage.getFilteredItems().keySet()) {
                        d.storage.unfilter(key);
                    }
                    d.player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().filterCleanedUp()));
                    updateInventory(uuid);
                });

        addSwitchButton(mask, ctrl.switchGui(),
                (uuid, event) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    GuiUtil.browseGUI(p, FilterGUI.this, event.isLeftClick());
                });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.MATERIAL, ctrl.sortByMaterial());
        putSortConfig(sortConfigMap, SortType.NAME, ctrl.sortByName());
        putSortConfig(sortConfigMap, SortType.QUANTITY, ctrl.sortByQuantity());
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

    private List<Button> getRepresentItems(FilterData session, Map<String, Object> section) {
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        Stream<Item> itemStream = session.storage.getFilteredItems().values().stream()
                .filter(item -> item != null && item.isLoaded());

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
        public final Player player;
        public final Storage storage;
        public SortType sort;
        public boolean orderSort = true;
        public boolean confirm;

        private FilterData(Player player, User user) {
            this.player = player;
            this.storage = user.getStorage();
            this.sort = BaseGUI.getDefaultSort(config.settings(), SortType.class);
        }
    }
}
