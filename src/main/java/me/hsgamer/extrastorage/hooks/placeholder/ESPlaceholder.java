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
        return "HyronicTeam";
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
        if (!player.isOnline()) return null;

        String argsLowerCase = args.toLowerCase();

        Storage storage = instance.getUserManager().getUser(player).getStorage();

        switch (argsLowerCase) {
            case "space":
                return (storage.getSpace() == -1) ? "-1" : Long.toString(storage.getSpace());
            case "space_formatted":
                return (storage.getSpace() == -1) ? "-1" : Digital.formatThousands(storage.getSpace());
            case "used_space":
                return Long.toString(storage.getUsedSpace());
            case "used_space_formatted":
                return Digital.formatThousands(storage.getUsedSpace());
            case "free_space":
                return (storage.getFreeSpace() == -1) ? "-1" : Long.toString(storage.getFreeSpace());
            case "free_space_formatted":
                return (storage.getFreeSpace() == -1) ? "-1" : Digital.formatThousands(storage.getFreeSpace());
            case "used_percent":
                return (storage.getSpaceAsPercent(true) == -1) ? "-1" : Double.toString(storage.getSpaceAsPercent(true));
            case "used_percent_formatted":
                return (storage.getSpaceAsPercent(true) == -1) ? "-1" : (storage.getSpaceAsPercent(true) + "%");
            case "free_percent":
                return (storage.getSpaceAsPercent(false) == -1) ? "-1" : Double.toString(storage.getSpaceAsPercent(false));
            case "free_percent_formatted":
                return (storage.getSpaceAsPercent(false) == -1) ? "-1" : (storage.getSpaceAsPercent(false) + "%");
        }

        if (argsLowerCase.startsWith("quantity")) {
            String key = args.substring(args.indexOf('_') + 1);
            boolean isFormatted = key.toUpperCase().startsWith("FORMATTED");
            if (isFormatted) key = key.substring(key.indexOf('_') + 1);

            Optional<Item> item = storage.getItem(key);
            if (!item.isPresent()) item = storage.getItem(key.toUpperCase());
            if (!item.isPresent()) return "-1";

            if (isFormatted) return Digital.formatThousands(Math.min(item.get().getQuantity(), Integer.MAX_VALUE));
            else return Long.toString(item.get().getQuantity());
        }

        return null;
    }

}
