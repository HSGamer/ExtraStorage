package me.hsgamer.extrastorage.gui;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.gui.base.ESGui;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class WhitelistGui
        extends ESGui {

    private UserManager manager;
    private Setting setting;
    private List<String> items;
    private int[] slots;

    private WhitelistGui(Player player, int page, SortType sort, boolean orderSort) {
        super("gui/whitelist", player, page, sort, orderSort);

        if (player == null) return;

        this.manager = instance.getUserManager();
        this.setting = instance.getSetting();
        this.items = this.sortList(setting.getWhitelist());
        this.slots = this.getSlots("RepresentItem");

        this.handleClick(event -> {
            if (event.isTopClick()) return;
            event.setCancelled(true);

            final ItemStack item = event.getClickedItem();
            if ((item == null) || (item.getType() == Material.AIR)) return;

            this.playSoundIfPresent();

            final String validKey = ItemUtil.toMaterialKey(item);
            if (validKey.equals(Constants.INVALID)) {
                player.sendMessage(Message.getMessage("FAIL.invalid-item"));
                return;
            }
            if (setting.getBlacklist().contains(validKey)) {
                player.sendMessage(Message.getMessage("FAIL.item-blacklisted"));
                return;
            }
            if (setting.getWhitelist().contains(validKey)) {
                player.sendMessage(Message.getMessage("FAIL.item-already-whitelisted"));
                return;
            }

            setting.addToWhitelist(validKey);
            for (User user : manager.getUsers()) {
                Storage storage = user.getStorage();
                Optional<Item> optional = storage.getItem(validKey);
                if (!optional.isPresent()) storage.addNewItem(validKey);
            }

            player.sendMessage(Message.getMessage("SUCCESS.item-added-to-whitelist").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(validKey, true)));

            this.reopenGui(page);
        });

        this.load();
    }

    public WhitelistGui(Player player, int page) {
        this(player, page, SortType.NAME, true);
    }


    @Override
    public void reopenGui(int page) {
        new WhitelistGui(player, page, sort, orderSort).open();
    }

    @Override
    public void reopenGui(int page, SortType sort, boolean order) {
        new WhitelistGui(player, page, sort, order).open();
    }


    private void load() {
        this.addDecorateItems();
        this.addSortByName();
        this.addRepresentItem();
    }

    private void addRepresentItem() {
        int index = 0, startIndex, endIndex;
        endIndex = Math.min(items.size(), page * slots.length);
        for (startIndex = (page - 1) * slots.length; startIndex < endIndex; startIndex++) {
            String key = items.get(startIndex);
            ItemUtil.ItemPair pair = ItemUtil.getItem(key);
            if (pair.type() == ItemUtil.ItemType.NONE) continue;

            ItemStack iStack = pair.item();

            ItemMeta meta = iStack.getItemMeta();

            String name = config.getString("RepresentItem.Name", "");
            if (!name.isEmpty()) meta.setDisplayName(name);
            else meta.setDisplayName(setting.getNameFormatted(key, true));

            List<String> curLore = (meta.hasLore() ? meta.getLore() : new ArrayList<>()), newLore = config.getStringList("RepresentItem.Lore");
            if (!newLore.isEmpty()) {
                curLore.addAll(newLore);
                meta.setLore(curLore);
            }
            iStack.setItemMeta(meta);

            Icon icon = new Icon(iStack)
                    .handleClick(event -> {
                        this.playSoundIfPresent();

                        setting.removeFromWhitelist(key);

                        for (User user : manager.getUsers()) {
                            Storage storage = user.getStorage();
                            Optional<Item> optional = storage.getItem(key);
                            if (optional.isPresent()) storage.unfilter(key);
                        }
                        player.sendMessage(Message.getMessage("SUCCESS.item-removed-from-whitelist").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(key, true)));

                        this.reopenGui(1);
                    })
                    .setSlots(slots[index++]);
            this.addIcon(icon);
        }
        int maxPages = (int) Math.ceil((double) items.size() / slots.length);

        if (page > 1) this.addPreviousButton(maxPages);
        if (page < maxPages) this.addNextButton(maxPages);
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
            boolean left = event.isLeftClick();
            if (orderSort == left) return;

            this.playSoundIfPresent();
            this.reopenGui(page, sort, left);
        }).setSlots(slots);
        this.addIcon(icon);
    }

    private List<String> sortList(List<String> unsort) {
        if (unsort.isEmpty() || (unsort.size() == 1)) return unsort;
        List<String> sorted = new LinkedList<>(unsort);
        sorted.sort((object1, object2) -> {
            if (orderSort) return object1.compareTo(object2);
            return object2.compareTo(object1);
        });
        return sorted;
    }

}
