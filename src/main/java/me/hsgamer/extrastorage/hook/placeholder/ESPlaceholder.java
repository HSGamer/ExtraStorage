package me.hsgamer.extrastorage.hook.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.SortUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ESPlaceholder extends PlaceholderExpansion {

    private final ExtraStorage instance;

    public ESPlaceholder(ExtraStorage instance) {
        this.instance = instance;
    }

    @Override
    public String getIdentifier() {
        return "exstorage";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", instance.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return instance.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String args) {
        if (player == null) return null;

        String[] parts = args.split("_", 3);
        String first = parts[0].toLowerCase(Locale.ROOT);

        switch (first) {
            case "quantity":
                String key = parts.length > 1 ? parts[1] : "";
                boolean formatted = key.equalsIgnoreCase("formatted");
                if (formatted && parts.length > 2) key = parts[2];
                return getQuantity(key, formatted, player);
            case "item":
                if (parts.length < 3 || parts[2].isEmpty()) return null;
                return getItemPlaceholder(parts[1].toLowerCase(Locale.ROOT), parts[2], player);
            case "items":
                String type = parts.length > 1 ? parts[1] : null;
                return getItems(type, player);
        }
        return getStoragePlaceholder(args.toLowerCase(Locale.ROOT), player);
    }

    private String getItems(String type, OfflinePlayer player) {
        Storage storage = getPlayerStorage(player);
        Stream<Item> itemStream = storage.getItems().values().stream()
                .filter(item -> item != null && item.isLoaded());

        if (type == null || type.isEmpty()) {
            return itemStream.map(Item::getKey).collect(Collectors.joining("\n"));
        }

        if (type.equalsIgnoreCase("unfilter")) {
            return itemStream.filter(item -> !item.isFiltered()).map(Item::getKey).collect(Collectors.joining("\n"));
        }

        Comparator<Item> comparator = getItemComparator(type);
        if (comparator == null) return null;
        return itemStream
                .filter(item -> item.isFiltered() || item.getQuantity() > 0)
                .sorted(comparator)
                .map(Item::getKey)
                .collect(Collectors.joining("\n"));
    }

    private Comparator<Item> getItemComparator(String type) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "material": {
                Comparator<Item> comparator = SortUtil::compareItemByMaterial;
                return comparator.thenComparing(SortUtil::compareItemByQuantity);
            }
            case "name": {
                Comparator<Item> comparator = SortUtil::compareItemByName;
                return comparator.thenComparing(SortUtil::compareItemByQuantity);
            }
            case "quantity":
                return SortUtil::compareItemByQuantity;
            default:
                return null;
        }
    }

    private String getQuantity(String key, boolean formatted, OfflinePlayer player) {
        Optional<Item> item = getPlayerStorage(player).getItem(key);
        if (!item.isPresent()) return "-1";

        long quantity = item.get().getQuantity();
        return formatted ? Digital.formatThousands(Math.min(quantity, Integer.MAX_VALUE)) : Long.toString(quantity);
    }

    private String getItemPlaceholder(String type, String key, OfflinePlayer player) {
        switch (type) {
            case "quantity":
                return getItemQuantity(key, getPlayerStorage(player));
            case "status":
                return getItemStatus(key, getPlayerStorage(player));
            default:
                return getItemInfo(key, type);
        }
    }

    private String getItemQuantity(String key, Storage storage) {
        Optional<Item> item = storage.getItem(key);
        if (!item.isPresent()) return "-1";
        return Digital.formatThousands(item.get().getQuantity());
    }

    private String getItemStatus(String key, Storage storage) {
        Optional<Item> item = storage.getItem(key);
        return item.map(value -> Utils.formatMessage(value.isFiltered()
                        ? instance.get(MessageConfig.class).status().filtered()
                        : instance.get(MessageConfig.class).status().unfiltered()))
                .orElse(null);
    }

    private String getItemInfo(String key, String type) {
        io.github.projectunified.uniitem.api.Item item = ItemUtil.getItem(key);
        if (!item.isValid()) return null;
        ItemStack itemStack = item.bukkitItem();
        if (itemStack == null) return null;

        switch (type) {
            case "name":
                return instance.get(SettingConfig.class).getNameFormatted(key, true);
            case "material":
                return itemStack.getType().name();
            case "lore": {
                ItemMeta meta = itemStack.getItemMeta();
                List<String> lore = meta == null ? null : meta.getLore();
                return lore == null ? "" : String.join("\n", lore);
            }
            default:
                return null;
        }
    }

    private Storage getPlayerStorage(OfflinePlayer player) {
        return instance.get(UserManager.class).getUser(player).getStorage();
    }

    private String getStoragePlaceholder(String arg, OfflinePlayer player) {
        Storage storage = getPlayerStorage(player);
        switch (arg) {
            case "space":
                return formatNumber(storage.getSpace());
            case "space_formatted":
                return formatThousands(storage.getSpace());
            case "used_space":
                return formatNumber(storage.getUsedSpace());
            case "used_space_formatted":
                return formatThousands(storage.getUsedSpace());
            case "free_space":
                return formatNumber(storage.getFreeSpace());
            case "free_space_formatted":
                return formatThousands(storage.getFreeSpace());
            case "used_percent":
                return formatPercent(storage.getSpaceAsPercent(true));
            case "used_percent_formatted":
                return formatPercentFormatted(storage.getSpaceAsPercent(true));
            case "free_percent":
                return formatPercent(storage.getSpaceAsPercent(false));
            case "free_percent_formatted":
                return formatPercentFormatted(storage.getSpaceAsPercent(false));
            case "status":
                return Boolean.toString(storage.getStatus());
            case "status_formatted":
                return Utils.formatMessage(storage.getStatus()
                        ? instance.get(MessageConfig.class).status().enabled()
                        : instance.get(MessageConfig.class).status().disabled());
            default:
                return null;
        }
    }

    private String formatNumber(long value) {
        return (value == -1) ? "-1" : Long.toString(value);
    }

    private String formatThousands(long value) {
        return (value == -1) ? "-1" : Digital.formatThousands(value);
    }

    private String formatPercent(double percent) {
        return (percent == -1) ? "-1" : Double.toString(percent);
    }

    private String formatPercentFormatted(double percent) {
        return (percent == -1) ? "-1" : (percent + "%");
    }
}
