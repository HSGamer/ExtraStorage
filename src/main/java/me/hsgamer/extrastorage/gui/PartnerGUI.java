package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.ActionItem;
import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.HybridMask;
import io.github.projectunified.craftux.simple.SimpleButtonMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartnerGUI extends BaseGUI<PartnerGUI.SortType> {
    private boolean confirm;

    public PartnerGUI(Player player, GuiConfig config) {
        super(player, config, SortType.class);
        this.confirm = false;

        setup();
    }

    @Override
    protected SortType fallbackSort() {
        return SortType.NAME;
    }

    @Override
    protected List<Button> getRepresentItems(org.bukkit.configuration.ConfigurationSection section) {
        Stream<Partner> partnerStream = user.getPartners().stream();

        partnerStream = partnerStream.sorted((obj1, obj2) -> {
            int compare = 0;
            OfflinePlayer p1 = obj1.getOfflinePlayer(), p2 = obj2.getOfflinePlayer();
            switch (sort) {
                case NAME:
                    if (orderSort) {
                        compare = p1.getName().compareTo(p2.getName());
                        if (compare == 0) compare = Long.compare(obj2.getTimestamp(), obj1.getTimestamp());
                    } else {
                        compare = p2.getName().compareTo(p1.getName());
                        if (compare == 0) compare = Long.compare(obj1.getTimestamp(), obj2.getTimestamp());
                    }
                    break;
                case TIME:
                    if (orderSort) {
                        compare = Long.compare(obj2.getTimestamp(), obj1.getTimestamp());
                        if (compare == 0) compare = p1.getName().compareTo(p2.getName());
                    } else {
                        compare = Long.compare(obj1.getTimestamp(), obj2.getTimestamp());
                        if (compare == 0) compare = p2.getName().compareTo(p1.getName());
                    }
                    break;
            }
            return compare;
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
                        if (holder instanceof StorageGui) {
                            StorageGui gui = (StorageGui) holder;
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
    protected Mask getControlItems(org.bukkit.configuration.ConfigurationSection section) {
        HybridMask mask = new HybridMask();

        ConfigurationSection aboutItemSection = Objects.requireNonNull(section.getConfigurationSection("About"), "ControlItems.About must be non-null!");
        GUIItem aboutItem = GUIItem.get(aboutItemSection, null);
        List<Position> aboutItemSlots = getSlots(aboutItemSection);

        ConfigurationSection sortByNameSection = Objects.requireNonNull(section.getConfigurationSection("SortByName"), "ControlItems.SortByName must be non-null!");
        GUIItem sortByNameItem = GUIItem.get(sortByNameSection, null);
        List<Position> sortByNameSlots = getSlots(sortByNameSection);

        ConfigurationSection sortByTimeSection = Objects.requireNonNull(section.getConfigurationSection("SortByTime"), "ControlItems.SortByTime must be non-null!");
        GUIItem sortByTimeItem = GUIItem.get(sortByTimeSection, null);
        List<Position> sortByTimeSlots = getSlots(sortByTimeSection);

        ConfigurationSection switchSection = Objects.requireNonNull(section.getConfigurationSection("SwitchGui"), "ControlItems.SwitchGui must be non-null!");
        GUIItem switchGuiItem = GUIItem.get(switchSection, null);
        List<Position> switchSlots = getSlots(switchSection);

        SimpleButtonMask aboutMask = new SimpleButtonMask();
        mask.add(aboutMask);
        aboutMask.setButton(aboutItemSlots, (uuid, actionItem) -> {
            aboutItem.apply(actionItem, user, s -> s.replaceAll(Utils.getRegex("total(\\_|\\-)partners"), Integer.toString(user.getPartners().size())));
            actionItem.setAction(InventoryClickEvent.class, event -> {
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
                    if (holder instanceof StorageGui) {
                        StorageGui gui = (StorageGui) holder;
                        if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                    }
                }
                user.clearPartners();
                player.sendMessage(Message.getMessage("SUCCESS.cleanup-partners-list"));

                updateRepresentItems();
                update();
            });
            return true;
        });

        SimpleButtonMask switchMask = new SimpleButtonMask();
        mask.add(switchMask);
        switchMask.setButton(switchSlots, (uuid, actionItem) -> {
            switchGuiItem.apply(actionItem, user, s -> s);
            actionItem.setAction(InventoryClickEvent.class, event -> {
                // TODO: Implement the new switch on my own
                if (event.isLeftClick()) {
                    if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                } else if (event.isRightClick()) {
                    if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                    else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                }
            });
            return true;
        });

        Mask sortMask = new Mask() {
            @Override
            public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                Map<Position, Consumer<ActionItem>> map = new HashMap<>();

                GUIItem displayItem = null;
                List<Position> positions = new ArrayList<>();
                SortType nextSort = null;
                SortType previousSort = null;
                switch (sort) {
                    case NAME: {
                        displayItem = sortByNameItem;
                        nextSort = SortType.TIME;
                        previousSort = SortType.TIME;
                        positions = sortByNameSlots;
                        break;
                    }
                    case TIME: {
                        displayItem = sortByTimeItem;
                        nextSort = SortType.NAME;
                        previousSort = SortType.NAME;
                        positions = sortByTimeSlots;
                        break;
                    }
                }

                if (displayItem != null && !positions.isEmpty()) {
                    GUIItem finalDisplayItem = displayItem;
                    SortType finalNextSort = nextSort;
                    SortType finalPreviousSort = previousSort;
                    Consumer<ActionItem> actionItemConsumer = actionItem -> {
                        finalDisplayItem.apply(actionItem, user, s -> s);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            if (event.isShiftClick()) {
                                orderSort = !orderSort;
                            } else {
                                SortType newSort = event.isLeftClick() ? finalNextSort : (event.isRightClick() ? finalPreviousSort : null);
                                if (newSort == null) return;
                                sort = newSort;
                            }
                            updateRepresentItems();
                            update();
                        });
                    };
                    positions.forEach(position -> map.put(position, actionItemConsumer));
                }
                return map;
            }
        };
        mask.add(sortMask);

        return mask;
    }

    public enum SortType {
        NAME, TIME
    }
}
