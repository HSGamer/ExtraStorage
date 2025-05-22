package me.hsgamer.extrastorage.commands.abstraction;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.command.CommandSender;

abstract class CommandBase {

    protected final ExtraStorage instance;
    protected final String VERSION_REGEX, LABEL_REGEX, USAGE_REGEX, VALUE_REGEX;

    CommandBase() {
        this.instance = ExtraStorage.getInstance();

        this.VERSION_REGEX = Utils.getRegex("ver(sion)?");
        this.LABEL_REGEX = Utils.getRegex("label");
        this.USAGE_REGEX = Utils.getRegex("usage");
        this.VALUE_REGEX = Utils.getRegex("value");
    }

    protected final boolean hasPermission(CommandSender sender, String perm) {
        return (sender.isOp() || sender.hasPermission(perm));
    }

}
