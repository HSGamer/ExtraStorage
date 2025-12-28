package me.hsgamer.extrastorage.gui;

import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.ESGui;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public final class PartnerGui
        extends ESGui {

    private List<Partner> partners;
    private int[] slots;

    private boolean confirm;

    private PartnerGui(Player player, int page, SortType sort, boolean order) {
        super("gui/partner", player, page, sort, order);

        if (player == null) return;

        this.partners = this.sortPartnerList(user.getPartners());
        this.slots = this.getSlots("RepresentItem");

        this.confirm = false;

        this.handleClick(event -> {
            if (!event.isTopClick()) event.setCancelled(true);
        });

        this.load();
    }

    public PartnerGui(Player player, int page) {
        super("gui/partner", player, page);

        if (player == null) return;

        this.partners = this.sortPartnerList(user.getPartners());
        this.slots = this.getSlots("RepresentItem");

        this.confirm = false;

        this.handleClick(event -> {
            if (!event.isTopClick()) event.setCancelled(true);
        });

        this.load();
    }


    @Override
    public void reopenGui(int page) {
        new PartnerGui(player, page, sort, orderSort).open();
    }

    @Override
    public void reopenGui(int page, SortType sort, boolean order) {
        new PartnerGui(player, page, sort, order).open();
    }


    private void load() {
        this.addDecorateItems();

        switch (sort) {
            case NAME:
                this.addSortByName();
                break;
            case TIME:
                this.addSortByTime();
                break;
        }

        this.addSwitchButton();
        this.addRepresentItem();
        this.addAboutItem();
    }

    private void addRepresentItem() {
        final String PATH = "RepresentItem";

        int index = 0, startIndex, endIndex;
        endIndex = Math.min(partners.size(), page * slots.length);
        for (startIndex = (page - 1) * slots.length; startIndex < endIndex; startIndex++) {
            Partner partner = partners.get(startIndex);
            OfflinePlayer pnPlayer = partner.getOfflinePlayer();
            User user = instance.getUserManager().getUser(pnPlayer);

            ItemStack item = this.getItemStack(
                    PATH,
                    user,
                    s -> {
                        if (s.matches(Utils.getRegex("partner"))) {
                            return user.getTexture();
                        }
                        return s.replaceAll(Utils.getRegex("partner"), pnPlayer.getName())
                                .replaceAll(Utils.getRegex("time(stamp)?"), partner.getTimeFormatted());
                    }
            );

            Icon icon = new Icon(item)
                    .handleClick(event -> {
                        this.playSoundIfPresent();

                        this.user.removePartner(pnPlayer.getUniqueId());
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

                        this.reopenGui(1);
                    }).setSlots(slots[index++]);
            this.addIcon(icon);
        }
        int maxPages = (int) Math.ceil((double) partners.size() / slots.length);

        if (page > 1) this.addPreviousButton(maxPages);
        if (page < maxPages) this.addNextButton(maxPages);
    }

    private void addAboutItem() {
        int[] slots = this.getSlots("ControlItems.About");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.About";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        user,
                        s -> s.replaceAll(Utils.getRegex("total(\\_|\\-)partners"), Integer.toString(partners.size()))
                )
        ).handleClick(event -> {
            if (partners.isEmpty() || (!event.isShiftClick())) return;

            this.playSoundIfPresent();

            if (!confirm) {
                confirm = true;
                player.sendMessage(Message.getMessage("WARN.confirm-cleanup"));
                return;
            }

            for (Partner pn : partners) {
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

            this.reopenGui(1);
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private void addSortByName() {
        int[] slots = this.getSlots("ControlItems.SortByName");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.SortByName";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        user
                )
        ).handleClick(event -> {
            if (partners.isEmpty()) return;

            SortType newSort = (event.isShiftClick() ? sort : SortType.TIME);

            this.playSoundIfPresent();
            this.reopenGui(page, newSort, !event.isShiftClick());
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private void addSortByTime() {
        int[] slots = this.getSlots("ControlItems.SortByTime");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.SortByTime";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        user
                )
        ).handleClick(event -> {
            if (partners.isEmpty()) return;

            SortType newSort = (event.isShiftClick() ? sort : SortType.NAME);

            this.playSoundIfPresent();
            this.reopenGui(page, newSort, !event.isShiftClick());
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private void addSwitchButton() {
        int[] slots = this.getSlots("ControlItems.SwitchGui");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.SwitchGui";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        user
                )
        ).handleClick(event -> {
            this.playSoundIfPresent();

            if (event.isLeftClick()) {
                if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
            } else if (event.isRightClick()) {
                if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
            }
        }).setSlots(slots);
        this.addIcon(icon);
    }


    private List<Partner> sortPartnerList(Collection<Partner> unsort) {
        if (unsort.isEmpty() || (unsort.size() < 2)) return new ArrayList<>(unsort);
        List<Partner> entries = new LinkedList<>(unsort);
        entries.sort((obj1, obj2) -> {
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
        return entries;
    }

}
