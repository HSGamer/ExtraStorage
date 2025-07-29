package me.hsgamer.extrastorage.gui.base;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.builder.ItemBuilder;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.gui.abstraction.GuiCreator;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ESGui
        extends GuiCreator {

    protected Player player;
    protected User user;
    protected Storage storage;
    protected int page;
    protected SortType sort;
    protected boolean orderSort;
    private Setting setting;

    public ESGui(String fileName, Player player, int page, SortType sort, boolean orderSort) {
        super(fileName, player);

        if (player == null) return;

        this.setting = instance.getSetting();

        this.player = player;
        this.user = instance.getUserManager().getUser(player);
        this.storage = user.getStorage();

        this.page = page;
        this.sort = sort;
        this.orderSort = orderSort;
    }

    public ESGui(String fileName, Player player, int page) {
        super(fileName, player);

        if (player == null) return;

        this.setting = instance.getSetting();

        this.player = player;
        this.user = instance.getUserManager().getUser(player);
        this.storage = user.getStorage();

        this.page = page;

        try {
            this.sort = SortType.valueOf(config.getString("Settings.DefaultSort", "__INVALID_SORT__").toUpperCase());
        } catch (IllegalArgumentException ignored) {
            this.sort = SortType.MATERIAL;
        }
        this.orderSort = true;
    }

    public abstract void reopenGui(int page);

    public abstract void reopenGui(int page, SortType sort, boolean order);

    protected final void addPreviousButton(int maxPage) {
        int[] slots = this.getSlots("ControlItems.PreviousPage");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.PreviousPage";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) {
                                for (int i = 0; i < lores.size(); i++) {
                                    String lore = lores.get(i)
                                            .replaceAll(Utils.getRegex("page(s)?"), Integer.toString(page))
                                            .replaceAll(Utils.getRegex("max(\\_|\\-)?page(s)?"), Integer.toString(maxPage));
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
            this.playSoundIfPresent();

            if (page < 2) return;
            this.reopenGui(--page);
        }).setSlots(slots);
        this.addIcon(icon);
    }

    protected final void addNextButton(int maxPage) {
        int[] slots = this.getSlots("ControlItems.NextPage");
        if ((slots == null) || (slots.length < 1)) return;

        final String PATH = "ControlItems.NextPage";

        Icon icon = new Icon(
                this.getItemStack(
                        PATH,
                        meta -> {
                            String name = config.getString(PATH + ".Name", "");
                            if (!name.isEmpty()) meta.setDisplayName(name);

                            List<String> lores = config.getStringList(PATH + ".Lore");
                            if (!lores.isEmpty()) {
                                for (int i = 0; i < lores.size(); i++) {
                                    String lore = lores.get(i)
                                            .replaceAll(Utils.getRegex("page(s)?"), Integer.toString(page))
                                            .replaceAll(Utils.getRegex("max(\\_|\\-)?page(s)?"), Integer.toString(maxPage));
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
            this.playSoundIfPresent();

            if (page >= maxPage) return;
            this.reopenGui(++page);
        }).setSlots(slots);
        this.addIcon(icon);
    }

    protected final void addDecorateItems() {
        for (String keys : config.getConfigurationSection("DecorateItems").getKeys(false)) {
            final String path = "DecorateItems." + keys;

            ItemStack item = this.getItemStack(
                    path,
                    meta -> {
                        String name = config.getString(path + ".Name", "");
                        if (!name.isEmpty()) meta.setDisplayName(name);

                        List<String> lores = config.getStringList(path + ".Lore");
                        if (!lores.isEmpty()) meta.setLore(lores);

                        if (config.contains(path + ".CustomModelData")) {
                            int model = config.getInt(path + ".CustomModelData");
                            meta.setCustomModelData(model);
                        }
                    }
            );
            if ((item == null) || (item.getType() == Material.AIR)) continue;

            Icon icon = new Icon(item);

            int[] slots = this.getSlots(path);
            if ((slots != null) && (slots.length > 0)) icon.setSlots(slots);

            this.addIcon(icon);
        }
    }

    protected final Map<String, Item> sortItem(Map<String, Item> unsort) {
        if (unsort.isEmpty() || (unsort.size() < 2)) return unsort;
        Map<String, Item> sorted = new LinkedHashMap<>();
        if (sort == SortType.UNFILTER) {
            // Phần này chỉ được sử dụng trong StorageGui và SellGui.
            for (Item item : storage.getItems().values()) {
                if (item.isFiltered()) continue;
                sorted.put(item.getKey(), item);
                unsort.remove(item.getKey());
            }
        }
        List<Map.Entry<String, Item>> entries = unsort.entrySet().stream()
                .filter(entry -> {
                    if (sort == SortType.UNFILTER) return true;
                    Item item = entry.getValue();
                    return item.isFiltered() || (item.getQuantity() > 0);
                })
                .sorted((obj1, obj2) -> {
                    int compare = 0;
                    switch (sort) {
                        case MATERIAL:
                            if (orderSort) {
                                compare = obj1.getKey().compareTo(obj2.getKey());
                                if (compare == 0)
                                    compare = Integer.compare((int) Math.min(obj2.getValue().getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj1.getValue().getQuantity(), Integer.MAX_VALUE));
                            } else {
                                compare = obj2.getKey().compareTo(obj1.getKey());
                                if (compare == 0)
                                    compare = Integer.compare((int) Math.min(obj1.getValue().getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj2.getValue().getQuantity(), Integer.MAX_VALUE));
                            }
                            break;
                        case NAME:
                            String name1 = setting.getNameFormatted(obj1.getKey(), false), name2 = setting.getNameFormatted(obj2.getKey(), false);
                            if (orderSort) {
                                compare = name1.compareTo(name2);
                                if (compare == 0)
                                    compare = Integer.compare((int) Math.min(obj2.getValue().getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj1.getValue().getQuantity(), Integer.MAX_VALUE));
                            } else {
                                compare = name2.compareTo(name1);
                                if (compare == 0)
                                    compare = Integer.compare((int) Math.min(obj1.getValue().getQuantity(), Integer.MAX_VALUE), (int) Math.min(obj2.getValue().getQuantity(), Integer.MAX_VALUE));
                            }
                            break;
                        case QUANTITY:
                            if (orderSort) {
                                compare = Long.compare(obj2.getValue().getQuantity(), obj1.getValue().getQuantity());
                                if (compare == 0) compare = obj1.getKey().compareTo(obj2.getKey());
                            } else {
                                compare = Long.compare(obj1.getValue().getQuantity(), obj2.getValue().getQuantity());
                                if (compare == 0) compare = obj2.getKey().compareTo(obj1.getKey());
                            }
                            break;
                    }
                    return compare;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        for (Map.Entry<String, Item> entry : entries) sorted.put(entry.getKey(), entry.getValue());
        return sorted;
    }


    protected final ItemStack getItemStack(String model, User user, Material material, int amount, short data, String texture, List<String> enchants, List<String> flags, Consumer<ItemMeta> meta) {
        return new ItemBuilder.Builder()
                .setModel(model)
                .setUser(user)
                .setMaterial(material)
                .setAmount(amount)
                .setData(data)
                .setTexture(texture)
                .setEnchantments(enchants)
                .setHideFlags(flags)
                .setMeta(meta)
                .build()
                .getItem();
    }

    protected final ItemStack getItemStack(String path, User user, Consumer<ItemMeta> meta) {
        return this.getItemStack(
                config.getString(path + ".Model"),
                user,
                Material.matchMaterial(config.getString(path + ".Material")),
                config.getInt(path + ".Amount"),
                (short) config.getInt(path + ".Data"),
                config.getString(path + ".Texture"),
                new ArrayList<>(),
                new ArrayList<>(),
                meta
        );
    }

    protected final ItemStack getItemStack(String path, Consumer<ItemMeta> meta) {
        return this.getItemStack(path, null, meta);
    }

    public enum SortType {
        MATERIAL, NAME, QUANTITY, TIME, UNFILTER
    }

}
