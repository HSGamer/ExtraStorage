package me.hsgamer.extrastorage.commands.abstraction;

import me.hsgamer.extrastorage.plugin.HyronicPlugin;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.command.CommandSender;

abstract class CommandBase<T extends HyronicPlugin> {

    protected final T instance;
    protected final String VERSION_REGEX, LABEL_REGEX, USAGE_REGEX, VALUE_REGEX;

    CommandBase() {
        this.instance = (T) HyronicPlugin.getInstance();

        this.VERSION_REGEX = Utils.getRegex("ver(sion)?");
        this.LABEL_REGEX = Utils.getRegex("label");
        this.USAGE_REGEX = Utils.getRegex("usage");
        this.VALUE_REGEX = Utils.getRegex("value");
    }

    protected final boolean hasPermission(CommandSender sender, String perm) {
        return (sender.isOp() || sender.hasPermission(perm));
    }

}
