package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.util.SortUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartnerGUI extends BaseGUI<PartnerGUI.SortType> {
    private boolean confirm;

    public PartnerGUI(Player player) {
        super(player, ExtraStorage.getInstance().getPartnerGuiConfig(), SortType.class);
        this.confirm = false;

        setup();
    }


    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        Stream<Partner> partnerStream = user.getPartners().stream();

        partnerStream = partnerStream.sorted((obj1, obj2) -> {
            switch (sort) {
                case NAME:
                    return SortUtil.comparePartnerByName(obj1, obj2, orderSort);
                case TIME:
                    return SortUtil.comparePartnerByTimestamp(obj1, obj2, orderSort);
                default:
                    return 0;
            }
        });

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
                    user.removePartner(pnPlayer.getUniqueId());
                    player.sendMessage(Message.getMessage("SUCCESS.removed-partner").replaceAll(Utils.getRegex("player"), pnPlayer.getName()));
                    if (pnPlayer.isOnline()) {
                        Player p = pnPlayer.getPlayer();
                        p.sendMessage(Message.getMessage("SUCCESS.no-longer-partner").replaceAll(Utils.getRegex("player"), player.getName()));
                        InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                        if (holder instanceof StorageGUI) {
                            StorageGUI gui = (StorageGUI) holder;
                            if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                        }
                    }

                    updateRepresentItems();
                    update();
                });
                return true;
            };
        }).collect(Collectors.toList());
    }

    @Override
    protected Mask getControlItems(ConfigurationSection section) {
        HybridMask mask = new HybridMask();

        addAboutButton(mask, Objects.requireNonNull(section.getConfigurationSection("About")), s -> s.replaceAll(Utils.getRegex("total(\\_|\\-)partners"), Integer.toString(user.getPartners().size())), event -> {
            if (user.getPartners().isEmpty() || (!event.isShiftClick())) return;

            if (!confirm) {
                confirm = true;
                player.sendMessage(Message.getMessage("WARN.confirm-cleanup"));
                return;
            }

            for (Partner pn : user.getPartners()) {
                OfflinePlayer offPlayer = pn.getOfflinePlayer();
                if (!offPlayer.isOnline()) continue;

                Player p = offPlayer.getPlayer();
                p.sendMessage(Message.getMessage("SUCCESS.no-longer-partner").replaceAll(Utils.getRegex("player"), player.getName()));
                InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof StorageGUI) {
                    StorageGUI gui = (StorageGUI) holder;
                    if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                }
            }
            user.clearPartners();
            player.sendMessage(Message.getMessage("SUCCESS.cleanup-partners-list"));

            updateRepresentItems();
            update();
        });

        addSwitchButton(mask, Objects.requireNonNull(section.getConfigurationSection("SwitchGui")), event -> {
            browseGUI(event.isLeftClick());
        });

        Map<SortType, SortButtonConfig<SortType>> sortConfigMap = new EnumMap<>(SortType.class);
        putSortConfig(sortConfigMap, SortType.NAME, section, "SortByName");
        putSortConfig(sortConfigMap, SortType.TIME, section, "SortByTime");
        addSortMask(mask, sortConfigMap);

        return mask;
    }

    private void putSortConfig(Map<SortType, SortButtonConfig<SortType>> map, SortType type, ConfigurationSection section, String key) {
        ConfigurationSection subSection = section.getConfigurationSection(key);
        if (subSection == null) return;
        map.put(type, new SortButtonConfig<>(GUIItem.get(subSection, null), getSlots(subSection)));
    }

    public enum SortType {
        NAME, TIME
    }
}
