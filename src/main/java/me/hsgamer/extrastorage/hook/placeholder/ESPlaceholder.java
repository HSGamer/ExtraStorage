package me.hsgamer.extrastorage.hook.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.Optional;

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

        String arg = args.toLowerCase(Locale.ROOT);

        if (arg.startsWith("quantity")) {
            String key = args.substring(args.indexOf('_') + 1);
            boolean formatted = key.regionMatches(true, 0, "formatted", 0, 9);
            if (formatted) {
                int index = key.indexOf('_');
                key = index < 0 ? key : key.substring(index + 1);
            }

            Storage storage = instance.get(UserManager.class).getUser(player).getStorage();
            Optional<Item> item = storage.getItem(key);
            if (!item.isPresent()) return "-1";

            long quantity = item.get().getQuantity();
            return formatted ? Digital.formatThousands(Math.min(quantity, Integer.MAX_VALUE)) : Long.toString(quantity);
        }

        Storage storage = instance.get(UserManager.class).getUser(player).getStorage();
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
