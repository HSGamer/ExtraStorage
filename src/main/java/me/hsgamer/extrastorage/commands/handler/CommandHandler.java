package me.hsgamer.extrastorage.commands.handler;

import me.hsgamer.extrastorage.commands.abstraction.AbstractCommand;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.configs.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class CommandHandler
        extends AbstractCommand {

    @Override
    public boolean check(CommandSender sender, Command cmd, String label, String[] args) {
        if ((!cmd.permission().isEmpty()) && (!sender.hasPermission(cmd.permission()))) {
            sender.sendMessage(Message.getMessage("FAIL.no-permission"));
            return false;
        }

        if (args.length < cmd.minArgs()) {
            if (!cmd.usage().isEmpty())
                sender.sendMessage(Message.getMessage("FAIL.missing-args").replaceAll(USAGE_REGEX, cmd.usage().replaceAll(LABEL_REGEX, label)));
            else sender.sendMessage(Message.getMessage("FAIL.missing-args"));
            return false;
        }

        switch (cmd.target()) {
            case ONLY_PLAYER:
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Message.getMessage("FAIL.only-players"));
                    return false;
                }
                break;
            case ONLY_CONSOLE:
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(Message.getMessage("FAIL.only-console"));
                    return false;
                }
                break;
        }

        return true;
    }

}
