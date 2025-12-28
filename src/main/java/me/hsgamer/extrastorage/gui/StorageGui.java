package me.hsgamer.extrastorage.gui;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.gui.base.ESGui;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class StorageGui
        extends ESGui {

    private Setting setting;

    private User partner;
    private Storage storage;

    private Map<String, Item> items;
    private int[] slots;

    private StorageGui(Player player, User partner, int page, SortType sort, boolean order) {
        super("gui/storage", player, page, sort, order);

        if (player == null) return;

        this.setting = instance.getSetting();

        this.partner = ((partner == null) ? user : partner);
        this.storage = this.partner.getStorage();

        this.items = this.sortItem(storage.getItems());
        this.slots = this.getSlots("RepresentItem");

        this.handleClick(event -> {
            if (!event.isTopClick()) event.setCancelled(true);
        });

        this.load();
    }

    public StorageGui(Player player, User partner, int page) {
        super("gui/storage", player, page);

        if (player == null) return;

        this.setting = instance.getSetting();

        this.partner = ((partner == null) ? user : partner);
        this.storage = this.partner.getStorage();

        this.items = this.sortItem(storage.getItems());
        this.slots = this.getSlots("RepresentItem");

        this.handleClick(event -> {
            if (!event.isTopClick()) event.setCancelled(true);
        });

        this.load();
    }

    public StorageGui(Player player, int page) {
        this(player, null, page);
    }

    public static String getUserId() {
        return "%%__USER__%%";
    }

    public static String getResourceId() {
        return "%%__RESOURCE__%%";
    }

    public static String getUniqueId() {
        return "%%__NONCE__%%";
    }

    @Override
    public void reopenGui(int page) {
        new StorageGui(player, partner, page, sort, orderSort).open();
    }

    @Override
    public void reopenGui(int page, SortType sort, boolean order) {
        new StorageGui(player, partner, page, sort, order).open();
    }

    private void load() {
        this.addDecorateItems();

        switch (sort) {
            case MATERIAL:
                this.addSortByMaterial();
                break;
            case NAME:
                this.addSortByName();
                break;
            case QUANTITY:
                this.addSortByQuantity();
                break;
            case UNFILTER:
                this.addSortByUnfilter();
                break;
        }

        this.addSwitchButton();
        this.addRepresentItem();
        this.addAboutItem();
    }

    private void addRepresentItem() {
        int index = 0, startIndex, endIndex;
        Set<String> keys = items.keySet();
        endIndex = Math.min(items.size(), page * slots.length);
        for (startIndex = (page - 1) * slots.length; startIndex < endIndex; startIndex++) {
            String key = keys.toArray()[startIndex].toString();
            Item item = items.get(key);
            if (item == null || !item.isLoaded()) continue;

            ItemStack iStack = item.getItem().clone();
            ItemMeta meta = iStack.getItemMeta();

            String name = config.getString("RepresentItem.Name", "");
            if (!name.isEmpty()) meta.setDisplayName(name);
            else meta.setDisplayName(setting.getNameFormatted(key, true));

            List<String> curLore = (meta.hasLore() ? meta.getLore() : new ArrayList<>()), newLore = config.getStringList("RepresentItem.Lore");
            if (!newLore.isEmpty()) {
                for (int i = 0; i < newLore.size(); i++) {
                    String lore = newLore.get(i)
                            .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (item.isFiltered() ? "filtered" : "unfiltered")))
                            .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()));
                    newLore.set(i, lore);
                }
                curLore.addAll(newLore);
                meta.setLore(curLore);
            }
            iStack.setItemMeta(meta);

            Icon icon = new Icon(iStack)
                    .handleClick(event -> {
                        this.playSoundIfPresent();

                        ItemStack clicked = event.getClickedItem();
                        if ((clicked == null) || (clicked.getType() == Material.AIR)) return;

                        final ClickType click = event.getEvent().getClick();
                        if (click == ClickType.SHIFT_RIGHT) {
                            // Chuyển tất cả vật phẩm trong kho đồ của người chơi vào kho chứa:
                            if (storage.isMaxSpace()) {
                                player.sendMessage(Message.getMessage("FAIL.storage-is-full"));
                                return;
                            }
                            if (!item.isFiltered()) {
                                player.sendMessage(Message.getMessage("FAIL.item-not-filtered"));
                                return;
                            }
                            if (instance.getSetting().getBlacklist().contains(item.getKey()) || (instance.getSetting().isLimitWhitelist() && !instance.getSetting().getWhitelist().contains(item.getKey()))) {
                                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                                return;
                            }

                            int count = 0;
                            ItemStack[] items = player.getInventory().getStorageContents();
                            for (ItemStack is : items) {
                                if ((is == null) || (is.getType() == Material.AIR)) continue;

                                Optional<Item> optional = storage.getItem(is);
                                if (!optional.isPresent()) continue;
                                Item i = optional.get();
                                if (!i.isLoaded()) continue;

                                if (item.getType() != i.getType()) continue;
                                if (!item.getKey().equalsIgnoreCase(ItemUtil.toMaterialKey(is))) continue;

                                int amount = is.getAmount();
                                long freeSpace = storage.getFreeSpace();
                                if ((freeSpace == -1) || ((freeSpace - amount) >= 0)) {
                                    count += amount;
                                    storage.add(key, amount);
                                    player.getInventory().removeItem(is);
                                    continue;
                                }
                                amount = (int) freeSpace;
                                count += amount;
                                storage.add(key, amount);

                                if (is.getAmount() > amount) is.setAmount(is.getAmount() - amount);
                                else player.getInventory().removeItem(is);
                                break;
                            }
                            if (count == 0) {
                                player.sendMessage(Message.getMessage("FAIL.not-enough-item-in-inventory").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                                return;
                            }

                            if (instance.getSetting().isLogTransfer()) {
                                instance.getLog().log(player, partner.getOfflinePlayer(), Log.Action.TRANSFER, key, count, -1);
                            }

                            player.sendMessage(Message.getMessage("SUCCESS.moved-items-to-storage")
                                    .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(count))
                                    .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                            if (!partner.isOnline()) partner.save();

                            this.reopenGui(page);
                            return;
                        }

                        int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                        if (current <= 0) {
                            player.sendMessage(Message.getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));
                            return;
                        }

                        ItemStack vanillaItem = item.getItem();
                        if (click == ClickType.SHIFT_LEFT)
                            vanillaItem.setAmount(current);
                        else if (event.isLeftClick()) ; // Bỏ qua vì phần này chỉ rút 1 vật phẩm.
                        else if (event.isRightClick())
                            vanillaItem.setAmount(Math.min(current, clicked.getMaxStackSize()));
                        else return;
                        if (item.getType() == ItemUtil.ItemType.VANILLA) {
                            /*
                             * Cần xoá Meta của Item khi rút vì xảy ra trường hợp sau khi rút xong
                             * thì item sẽ có Meta, khiến cho việc drop ra mặt đất và không thể
                             * nhặt lại vào kho chứa được.
                             * Việc setItemMeta(null) sẽ không bị lỗi ở bất kỳ phiên bản nào.
                             */
                            vanillaItem.setItemMeta(null);
                        }

                        int free = this.getFreeSpace(vanillaItem);
                        if (free == -1) {
                            // Nếu kho đồ đã đầy:
                            player.sendMessage(Message.getMessage("FAIL.inventory-is-full"));
                            return;
                        }
                        vanillaItem.setAmount(free);

                        ItemUtil.giveItem(player, vanillaItem);
                        storage.subtract(key, free);

                        if (instance.getSetting().isLogWithdraw()) {
                            instance.getLog().log(player, partner.getOfflinePlayer(), Log.Action.WITHDRAW, key, free, -1);
                        }

                        if (!partner.isOnline()) partner.save();

                        player.sendMessage(Message.getMessage("SUCCESS.withdrew-item")
                                .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(free))
                                .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

                        this.reopenGui(page);
                    }).setSlots(slots[index++]);
            this.addIcon(icon);
        }
        int maxPages = (int) Math.ceil((double) items.size() / slots.length);

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
                        partner,
                        s -> {
                            String UNKNOWN = Message.getMessage("STATUS.unknown");

                            long space = storage.getSpace(), used = storage.getUsedSpace(), free = storage.getFreeSpace();
                            double usedPercent = storage.getSpaceAsPercent(true), freePercent = storage.getSpaceAsPercent(false);

                            return s
                                    .replaceAll(Utils.getRegex("player"), partner.getName())
                                    .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (storage.getStatus() ? "enabled" : "disabled")))
                                    .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                                    .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                                    .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                                    .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                                    .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
                        }
                )
        ).handleClick(event -> {
            boolean isAdminOrSelf = (this.hasPermission(Constants.ADMIN_OPEN_PERMISSION) || partner.getUUID().equals(player.getUniqueId()));
            if ((!this.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) || (!isAdminOrSelf)) return;

            this.playSoundIfPresent();

            boolean status = !storage.getStatus();
            storage.setStatus(status);

            player.sendMessage(Message.getMessage("SUCCESS.storage-usage-toggled").replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (status ? "enabled" : "disabled"))));

            this.reopenGui(page);
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private void addSortByMaterial() {
        int[] slots = this.getSlots("ControlItems.SortByMaterial");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.SortByMaterial";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        partner
                )
        ).handleClick(event -> {
            if (items.isEmpty()) return;

            SortType newSort = (event.isShiftClick() ? sort : (event.isLeftClick() ? SortType.NAME : (event.isRightClick() ? SortType.UNFILTER : null)));
            if (newSort == null) return;

            this.playSoundIfPresent();
            this.reopenGui(page, newSort, !event.isShiftClick());
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
                        partner
                )
        ).handleClick(event -> {
            if (items.isEmpty()) return;

            SortType newSort = (event.isShiftClick() ? sort : (event.isLeftClick() ? SortType.QUANTITY : (event.isRightClick() ? SortType.MATERIAL : null)));
            if (newSort == null) return;

            this.playSoundIfPresent();
            this.reopenGui(page, newSort, !event.isShiftClick());
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private void addSortByQuantity() {
        int[] slots = this.getSlots("ControlItems.SortByQuantity");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.SortByQuantity";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        partner
                )
        ).handleClick(event -> {
            if (items.isEmpty()) return;

            SortType newSort = (event.isShiftClick() ? sort : (event.isLeftClick() ? SortType.UNFILTER : (event.isRightClick() ? SortType.NAME : null)));
            if (newSort == null) return;

            this.playSoundIfPresent();
            this.reopenGui(page, newSort, !event.isShiftClick());
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private void addSortByUnfilter() {
        int[] slots = this.getSlots("ControlItems.SortByUnfilter");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.SortByUnfilter";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        partner
                )
        ).handleClick(event -> {
            if (items.isEmpty()) return;

            SortType newSort = (event.isShiftClick() ? sort : (event.isLeftClick() ? SortType.MATERIAL : (event.isRightClick() ? SortType.QUANTITY : null)));
            if (newSort == null) return;

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
                        partner
                )
        ).handleClick(event -> {
            this.playSoundIfPresent();

            if (event.isLeftClick()) {
                if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
            } else if (event.isRightClick()) {
                if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
            }
        }).setSlots(slots);
        this.addIcon(icon);
    }

    /*
     * Trả về khoảng trống còn lại trong kho đồ của người chơi:
     * Sẽ là -1 nếu không còn khoảng trống nào.
     */
    private int getFreeSpace(ItemStack item) {
        ItemStack[] items = player.getInventory().getStorageContents();
        int empty = 0;
        for (ItemStack stack : items) {
            if ((stack == null) || (stack.getType() == Material.AIR)) {
                empty += item.getMaxStackSize();
                continue;
            }
            if (!item.isSimilar(stack)) continue;
            empty += (stack.getMaxStackSize() - stack.getAmount());
        }
        if (empty > 0) return Math.min(empty, item.getAmount());
        return -1;
    }

    public User getPartner() {
        return this.partner;
    }
}
