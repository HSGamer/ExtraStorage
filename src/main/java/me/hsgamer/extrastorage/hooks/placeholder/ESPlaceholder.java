package me.hsgamer.extrastorage.hooks.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;

import me.hsgamer.extrastorage.util.Digital;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

public final class ESPlaceholder
        extends PlaceholderExpansion {

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
        String argsLowerCase = args.toLowerCase();

        Storage storage = instance.getUserManager().getUser(player).getStorage();

        long space = storage.getSpace();
        long usedSpace = storage.getUsedSpace();
        long freeSpace = storage.getFreeSpace();
        boolean status = storage.getStatus();

        switch (argsLowerCase) {
            case "space":
                return (space == -1) ? "-1" : Long.toString(space);
            case "space_formatted":
                return (space == -1) ? "-1" : Digital.formatThousands(space);
            case "used_space":
                return Long.toString(usedSpace);
            case "used_space_formatted":
                return Digital.formatThousands(usedSpace);
            case "free_space":
                return (freeSpace == -1) ? "-1" : Long.toString(freeSpace);
            case "free_space_formatted":
                return (freeSpace == -1) ? "-1" : Digital.formatThousands(freeSpace);
            case "used_percent": {
                double usedPercent = storage.getSpaceAsPercent(true);
                return (usedPercent == -1) ? "-1" : Double.toString(usedPercent);
            }
            case "used_percent_formatted": {
                double usedPercent = storage.getSpaceAsPercent(true);
                return (usedPercent == -1) ? "-1" : (usedPercent + "%");
            }
            case "free_percent": {
                double freePercent = storage.getSpaceAsPercent(false);
                return (freePercent == -1) ? "-1" : Double.toString(freePercent);
            }
            case "free_percent_formatted": {
                double freePercent = storage.getSpaceAsPercent(false);
                return (freePercent == -1) ? "-1" : (freePercent + "%");
            }
            case "status":
                return Boolean.toString(status);
            case "status_formatted":
                return ExtraStorage.getInstance().getMessage().getMessage("STATUS." + (status ? "enabled" : "disabled"));
        }

        if (argsLowerCase.startsWith("quantity")) {
            String key = args.substring(args.indexOf('_') + 1);
            boolean isFormatted = key.toUpperCase().startsWith("FORMATTED");
            if (isFormatted) key = key.substring(key.indexOf('_') + 1);

            Optional<Item> item = storage.getItem(key);
            if (!item.isPresent()) return "-1";

            if (isFormatted) return Digital.formatThousands(Math.min(item.get().getQuantity(), Integer.MAX_VALUE));
            else return Long.toString(item.get().getQuantity());
        }

        return null;
    }

}
