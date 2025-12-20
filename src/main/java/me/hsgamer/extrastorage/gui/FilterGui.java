package me.hsgamer.extrastorage.gui;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.ESGui;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class FilterGui
        extends ESGui {
    private Map<String, Item> items;
    private int[] slots;

    private boolean confirm;

    private FilterGui(Player player, int page, SortType sort, boolean order) {
        super("gui/filter", player, page, sort, order);

        if (player == null) return;

        this.items = this.sortItem(storage.getFilteredItems());
        this.slots = this.getSlots("RepresentItem");

        this.confirm = false;

        this.handleClick(event -> {
            if (event.isTopClick()) return;
            event.setCancelled(true);

            final ItemStack clickedItem = event.getClickedItem();
            if ((clickedItem == null) || (clickedItem.getType() == Material.AIR)) return;
            this.playSoundIfPresent();

            final String validKey = ItemUtil.toMaterialKey(clickedItem);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return;
            }
            if (storage.canStore(validKey)) return;

            if (instance.getSetting().getBlacklist().contains(validKey) || (instance.getSetting().isLimitWhitelist() && !instance.getSetting().getWhitelist().contains(validKey))) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return;
            }

            Optional<Item> optional = storage.getItem(validKey);
            if (optional.isPresent()) optional.get().setFiltered(true);
            else storage.addNewItem(validKey);

            this.reopenGui(page);
        });

        this.load();
    }

    public FilterGui(Player player, int page) {
        super("gui/filter", player, page);

        if (player == null) return;

        this.items = this.sortItem(storage.getFilteredItems());
        this.slots = this.getSlots("RepresentItem");

        this.confirm = false;

        this.handleClick(event -> {
            if (event.isTopClick()) return;
            event.setCancelled(true);

            final ItemStack clickedItem = event.getClickedItem();
            if ((clickedItem == null) || (clickedItem.getType() == Material.AIR)) return;
            this.playSoundIfPresent();

            final String validKey = ItemUtil.toMaterialKey(clickedItem);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return;
            }
            if (storage.canStore(validKey)) return;

            if (instance.getSetting().getBlacklist().contains(validKey) || (instance.getSetting().isLimitWhitelist() && !instance.getSetting().getWhitelist().contains(validKey))) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return;
            }

            Optional<Item> optional = storage.getItem(validKey);
            if (optional.isPresent()) optional.get().setFiltered(true);
            else storage.addNewItem(validKey);

            this.reopenGui(page);
        });

        this.load();
    }


    @Override
    public void reopenGui(int page) {
        new FilterGui(player, page, sort, orderSort).open();
    }

    @Override
    public void reopenGui(int page, SortType sort, boolean order) {
        new FilterGui(player, page, sort, order).open();
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
            else meta.setDisplayName(instance.getSetting().getNameFormatted(key, true));

            List<String> curLore = (meta.hasLore() ? meta.getLore() : new ArrayList<>()), newLore = config.getStringList("RepresentItem.Lore");
            if (!newLore.isEmpty()) {
                for (int i = 0; i < newLore.size(); i++) {
                    String lore = newLore.get(i).replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()));
                    newLore.set(i, lore);
                }
                curLore.addAll(newLore);
                meta.setLore(curLore);
            }
            iStack.setItemMeta(meta);

            Icon icon = new Icon(iStack)
                    .handleClick(event -> {
                        this.playSoundIfPresent();

                        storage.unfilter(key);

                        this.reopenGui(1);
                    })
                    .setSlots(slots[index++]);
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
                        user,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) {
                                String UNKNOWN = Message.getMessage("STATUS.unknown");

                                long space = storage.getSpace(), used = storage.getUsedSpace(), free = storage.getFreeSpace();
                                double usedPercent = storage.getSpaceAsPercent(true), freePercent = storage.getSpaceAsPercent(false);

                                for (int i = 0; i < lores.size(); i++) {
                                    String lore = lores.get(i)
                                            .replaceAll(Utils.getRegex("player"), player.getName())
                                            .replaceAll(Utils.getRegex("display"), player.getDisplayName())
                                            .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (storage.getStatus() ? "enabled" : "disabled")))
                                            .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                                            .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                                            .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                                            .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                                            .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
                                    lores.set(i, lore);
                                }
                                meta.setLore(lores);
                            }

                            if (config.contains(PATH + ".CustomModelData")) {
                                int modelData = config.getInt(PATH + ".CustomModelData");
                                meta.setCustomModelData(modelData);
                            }
                        }
                )
        ).handleClick(event -> {
            if (items.isEmpty() || (!event.isShiftClick())) return;

            this.playSoundIfPresent();

            if (!confirm) {
                confirm = true;
                player.sendMessage(Message.getMessage("WARN.confirm-cleanup"));
                return;
            }

            for (String key : items.keySet()) storage.unfilter(key);
            player.sendMessage(Message.getMessage("SUCCESS.filter-cleaned-up"));

            this.reopenGui(1);
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
                        user,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) meta.setLore(lores);

                            if (config.contains(PATH + ".CustomModelData")) {
                                int modelData = config.getInt(PATH + ".CustomModelData");
                                meta.setCustomModelData(modelData);
                            }
                        }
                )
        ).handleClick(event -> {
            SortType newSort = (event.isShiftClick() ? sort : (event.isLeftClick() ? SortType.NAME : (event.isRightClick() ? SortType.QUANTITY : null)));
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
                        user,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) meta.setLore(lores);

                            if (config.contains(PATH + ".CustomModelData")) {
                                int modelData = config.getInt(PATH + ".CustomModelData");
                                meta.setCustomModelData(modelData);
                            }
                        }
                )
        ).handleClick(event -> {
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
                        user,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) meta.setLore(lores);

                            if (config.contains(PATH + ".CustomModelData")) {
                                int modelData = config.getInt(PATH + ".CustomModelData");
                                meta.setCustomModelData(modelData);
                            }
                        }
                )
        ).handleClick(event -> {
            SortType newSort = (event.isShiftClick() ? sort : (event.isLeftClick() ? SortType.MATERIAL : (event.isRightClick() ? SortType.NAME : null)));
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
                        user,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) meta.setLore(lores);

                            if (config.contains(PATH + ".CustomModelData")) {
                                int modelData = config.getInt(PATH + ".CustomModelData");
                                meta.setCustomModelData(modelData);
                            }
                        }
                )
        ).handleClick(event -> {
            this.playSoundIfPresent();

            if (event.isLeftClick()) {
                if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
            } else if (event.isRightClick()) {
                if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_SELL_PERMISSION)) new SellGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
            }
        }).setSlots(slots);
        this.addIcon(icon);
    }

}
