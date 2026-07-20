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
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.WhitelistGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.item.GUIItemModifier;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class WhitelistGUI extends BaseGUI<WhitelistGUI.SortType, WhitelistGuiConfig> {

    private final Map<UUID, WhitelistData> sessions = new HashMap<>();

    public WhitelistGUI() {
        super("gui/whitelist.yml", WhitelistGuiConfig.class, SortType.class);
    }

    @Override
    protected void clearSessions() {
        sessions.clear();
    }

    public void openFor(Player player) {
        sessions.computeIfAbsent(player.getUniqueId(), k -> new WhitelistData(player));
        getInventory(player).open();
    }

    @Override
    protected void onBottomInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        WhitelistData data = sessions.get(player.getUniqueId());
        if (data == null) return;

        final ItemStack item = event.getCurrentItem();
        if ((item == null) || (item.getType() == Material.AIR)) return;

        final String validKey = ItemUtil.toMaterialKey(item);
        if (validKey.equals(Constants.INVALID)) {
            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().invalidItem()));
            return;
        }
        SettingConfig setting = ExtraStorage.getInstance().getSetting();
        if (setting.getNormalizedBlacklist().contains(validKey)) {
            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemBlacklisted()));
            return;
        }
        if (setting.getNormalizedWhitelist().contains(validKey)) {
            player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().fail().itemAlreadyWhitelisted()));
            return;
        }

        setting.addToWhitelist(validKey);
        for (User user : ExtraStorage.getInstance().getUserManager().getUsers()) {
            Storage storage = user.getStorage();
            Optional<Item> optional = storage.getItem(validKey);
            if (!optional.isPresent()) storage.addNewItem(validKey);
        }

        player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().itemAddedToWhitelist()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(validKey, true)));

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
                WhitelistData d = sessions.get(uuid);
                if (d == null) return Collections.emptyList();
                return getRepresentItems(d, representConfig);
            }
        };
        mask.add(repMask);

        WhitelistGuiConfig.WhitelistControlItemsConfig ctrl = config.controlItems();

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.NAME_NATURAL, ctrl.sortByName());
        putSortConfig(sortConfigMap, SortType.NAME_REVERSE, ctrl.sortByName());
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

    private List<Button> getRepresentItems(WhitelistData session, Map<String, Object> section) {
        SettingConfig setting = ExtraStorage.getInstance().getSetting();
        GUIItemModifier displayModifier = GUIItemModifier.getDisplayItemModifier(section, true);
        List<String> whitelist = new ArrayList<>(setting.getNormalizedWhitelist());

        Comparator<String> comparator;
        switch (session.sort) {
            case NAME_REVERSE:
                comparator = SortUtil.compose(session.orderSort, Comparator.<String>reverseOrder());
                break;
            case NAME_NATURAL:
            default:
                comparator = SortUtil.compose(session.orderSort, Comparator.<String>naturalOrder());
                break;
        }
        whitelist.sort(comparator);

        return whitelist.stream()
                .map(key -> {
                    io.github.projectunified.uniitem.api.Item item = ItemUtil.getItem(key);
                    if (!item.isValid()) return null;
                    ItemStack iStack = displayModifier.construct(item, key, UnaryOperator.identity());

                    return (Button) (uuid, actionItem) -> {
                        actionItem.setItem(iStack);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            setting.removeFromWhitelist(key);

                            for (User user : ExtraStorage.getInstance().getUserManager().getUsers()) {
                                Storage storage = user.getStorage();
                                Optional<Item> optional = storage.getItem(key);
                                if (optional.isPresent()) storage.unfilter(key);
                            }
                            session.player.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().itemRemovedFromWhitelist()).replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

                            updateInventory(uuid);
                        });
                        return true;
                    };
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public enum SortType {
        NAME_NATURAL, NAME_REVERSE
    }

    public class WhitelistData {
        public final Player player;
        public SortType sort;
        public boolean orderSort = true;

        private WhitelistData(Player player) {
            this.player = player;
        }
    }
}
