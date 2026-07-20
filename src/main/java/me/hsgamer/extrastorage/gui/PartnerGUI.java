package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.config.PartnerGuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.util.GuiUtil;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartnerGUI extends BaseGUI<PartnerGUI.SortType, PartnerGuiConfig> {

    private final Map<UUID, PartnerData> sessions = new HashMap<>();

    public PartnerGUI() {
        super("gui/partner.yml", PartnerGuiConfig.class, SortType.class);
    }

    @Override
    protected void clearSessions() {
        sessions.clear();
    }

    public void openFor(Player player) {
        PartnerData data = sessions.computeIfAbsent(player.getUniqueId(), PartnerData::new);
        data.confirm = false;
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
                PartnerData d = sessions.get(uuid);
                if (d == null) return Collections.emptyList();
                return getRepresentItems(d, representConfig);
            }
        };
        mask.add(repMask);

        PartnerGuiConfig.PartnerControlItemsConfig ctrl = config.controlItems();

        addAboutButton(mask, ctrl.about(),
                (uuid, text) -> {
                    PartnerData d = sessions.get(uuid);
                    return text.replaceAll(Utils.getRegex("total(\\_|\\-)partners"), Integer.toString(d.getUser().getPartners().size()));
                },
                (uuid, event) -> {
                    PartnerData d = sessions.get(uuid);
                    if (d.getUser().getPartners().isEmpty() || !event.isShiftClick()) return;
                    if (!d.confirm) {
                        d.confirm = true;
                        d.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().warn().confirmCleanup()));
                        return;
                    }
                    for (Partner pn : d.getUser().getPartners()) {
                        OfflinePlayer offPlayer = pn.getOfflinePlayer();
                        if (!offPlayer.isOnline()) continue;
                        Player p = offPlayer.getPlayer();
                        p.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().noLongerPartner()).replaceAll(Utils.getRegex("player"), d.getPlayer().getName()));
                        StorageGUI.StorageData sd = ExtraStorage.getInstance().getStorageGUI().getSessionData(p.getUniqueId());
                        if (sd != null && sd.partner.getUUID().equals(d.uuid)) p.closeInventory();
                    }
                    d.getUser().clearPartners();
                    d.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().cleanupPartnersList()));
                    updateInventory(uuid);
                });

        addSwitchButton(mask, ctrl.switchGui(),
                (uuid, event) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    GuiUtil.browseGUI(p, PartnerGUI.this, event.isLeftClick());
                });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.NAME, ctrl.sortByName());
        putSortConfig(sortConfigMap, SortType.TIME, ctrl.sortByTime());
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

    private List<Button> getRepresentItems(PartnerData session, Map<String, Object> section) {
        Stream<Partner> partnerStream = session.getUser().getPartners().stream();

        Comparator<Partner> comparator = null;
        switch (session.sort) {
            case NAME:
                comparator = SortUtil.compose(session.orderSort, SortUtil::comparePartnerByName);
                break;
            case TIME:
                comparator = SortUtil.compose(session.orderSort, SortUtil::comparePartnerByTimestamp);
                break;
            default:
                break;
        }
        if (comparator != null) {
            partnerStream = partnerStream.sorted(comparator);
        }

        GUIItem representItem = GUIItem.get(section, null);

        return partnerStream.map(partner -> {
            OfflinePlayer pnPlayer = partner.getOfflinePlayer();
            User partnerUser = ExtraStorage.getInstance().getUserManager().getUser(pnPlayer);

            ItemStack item = representItem.getItem(partnerUser, s -> {
                if (s.matches(Utils.getRegex("partner"))) {
                    String userTexture = partnerUser.getTexture();
                    return userTexture.isEmpty() ? partnerUser.getUUID().toString() : userTexture;
                }
                return s.replaceAll(Utils.getRegex("partner"), pnPlayer.getName())
                        .replaceAll(Utils.getRegex("time(stamp)?"), partner.getTimeFormatted());
            });

            return (Button) (uuid, actionItem) -> {
                actionItem.setItem(item);
                actionItem.setAction(InventoryClickEvent.class, event -> {
                    session.getUser().removePartner(pnPlayer.getUniqueId());
                    session.getPlayer().sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().removedPartner()).replaceAll(Utils.getRegex("player"), pnPlayer.getName()));
                    if (pnPlayer.isOnline()) {
                        Player p = pnPlayer.getPlayer();
                        p.sendMessage(Utils.formatMessage(ExtraStorage.getInstance().getMessage().success().noLongerPartner()).replaceAll(Utils.getRegex("player"), session.getPlayer().getName()));
                        StorageGUI.StorageData sd = ExtraStorage.getInstance().getStorageGUI().getSessionData(p.getUniqueId());
                        if (sd != null && sd.partner.getUUID().equals(session.uuid)) p.closeInventory();
                    }

                    updateInventory(uuid);
                });
                return true;
            };
        }).collect(Collectors.toList());
    }

    public enum SortType {
        NAME, TIME
    }

    public class PartnerData {
        public final UUID uuid;
        public SortType sort;
        public boolean orderSort = true;
        public boolean confirm;

        private PartnerData(UUID uuid) {
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
