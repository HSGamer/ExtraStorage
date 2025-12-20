package me.hsgamer.extrastorage.gui;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.ESGui;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.hooks.economy.EconomyProvider;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SellGui
        extends ESGui {

    private EconomyProvider econ;

    private Map<String, Item> items;
    private int[] slots;

    private SellGui(Player player, int page, SortType sort, boolean order) {
        super("gui/sell", player, page, sort, order);

        if (player == null) return;

        this.econ = instance.getSetting().getEconomyProvider();

        this.items = this.sortItem(storage.getItems());
        this.slots = this.getSlots("RepresentItem");

        this.handleClick(event -> {
            if (!event.isTopClick()) event.setCancelled(true);
        });

        this.load();
    }

    public SellGui(Player player, int page) {
        super("gui/sell", player, page);

        if (player == null) return;

        this.econ = instance.getSetting().getEconomyProvider();

        this.items = this.sortItem(storage.getItems());
        this.slots = this.getSlots("RepresentItem");

        this.handleClick(event -> {
            if (!event.isTopClick()) event.setCancelled(true);
        });

        this.load();
    }


    @Override
    public void reopenGui(int page) {
        new SellGui(player, page, sort, orderSort).open();
    }

    @Override
    public void reopenGui(int page, SortType sort, boolean order) {
        new SellGui(player, page, sort, order).open();
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
        int index = 0, pageCount = 1;
        for (Item item : items.values()) {
            if (item == null || !item.isLoaded()) continue;
            ItemStack iStack = item.getItem().clone();

            int amount = econ.getAmount(iStack);
            String price = econ.getPrice(player, iStack, amount);
            if ((amount < 1) || (price == null)) continue;

            if (pageCount != page) {
                if (++index >= slots.length) {
                    pageCount++;
                    index = 0;
                }
                continue;
            }

            ItemMeta meta = iStack.getItemMeta();

            String name = config.getString("RepresentItem.Name", "");
            if (!name.isEmpty()) meta.setDisplayName(name);
            else meta.setDisplayName(instance.getSetting().getNameFormatted(item.getKey(), true));

            List<String> curLore = (meta.hasLore() ? meta.getLore() : new ArrayList<>()), newLore = config.getStringList("RepresentItem.Lore");
            if (!newLore.isEmpty()) {
                for (int i = 0; i < newLore.size(); i++) {
                    String lore = newLore.get(i)
                            .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (item.isFiltered() ? "filtered" : "unfiltered")))
                            .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(item.getQuantity()))
                            .replaceAll(Utils.getRegex("price"), price)
                            .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(amount));
                    newLore.set(i, lore);
                }
                curLore.addAll(newLore);
                meta.setLore(curLore);
            }
            iStack.setItemMeta(meta);

            Icon icon = new Icon(iStack)
                    .handleClick(event -> {
                        this.playSoundIfPresent();

                        int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
                        if (current < 1) {
                            player.sendMessage(Message.getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(item.getKey(), true)));
                            return;
                        }

                        int sellAmount;
                        if (event.isShiftClick())
                            sellAmount = Digital.getBetween(1, Integer.MAX_VALUE, current);
                        else if (event.isLeftClick())
                            sellAmount = amount;
                        else if (event.isRightClick())
                            sellAmount = Digital.getBetween(1, current, iStack.getMaxStackSize());
                        else return;

                        instance.getSetting()
                                .getEconomyProvider()
                                .sellItem(player, item.getItem(), sellAmount, rs -> {
                                    if (!rs.isSuccess()) {
                                        player.sendMessage(Message.getMessage("FAIL.cannot-be-sold"));
                                        return;
                                    }
                                    storage.subtract(item.getKey(), rs.getAmount());
                                    player.sendMessage(Message.getMessage("SUCCESS.item-sold")
                                            .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(rs.getAmount()))
                                            .replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(item.getKey(), true))
                                            .replaceAll(Utils.getRegex("price"), Digital.formatDouble("###,###.##", rs.getPrice())));
                                });

                        this.reopenGui(page);
                    })
                    .setSlots(slots[index++]);
            this.addIcon(icon);

            if (index >= slots.length) {
                pageCount++;
                index = 0;
            }
        }
        if (page > 1) this.addPreviousButton(pageCount);
        if (page < pageCount) this.addNextButton(pageCount);
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
        ).setSlots(slots);
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
                if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
            } else if (event.isRightClick()) {
                if (this.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) new StorageGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) new FilterGui(player, 1).open();
                else if (this.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) new PartnerGui(player, 1).open();
            }
        }).setSlots(slots);
        this.addIcon(icon);
    }

}
