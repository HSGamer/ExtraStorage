package me.hsgamer.extrastorage.commands.subs.player;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;

@Command(value = {"help", "?"}, permission = Constants.PLAYER_HELP_PERMISSION)
public final class HelpCmd
        extends CommandListener {

    @Override
    public void execute(CommandContext context) {
        context.sendMessage(Message.getMessage("HELP.header").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
        context.sendMessage(Message.getMessage("HELP.Player.help").replaceAll(LABEL_REGEX, context.getLabel()));
        if (context.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Player.open").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Player.toggle").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Player.filter").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Player.partner").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.PLAYER_SELL_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Player.sell").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.PLAYER_WITHDRAW_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Player.withdraw").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        context.sendMessage(Message.getMessage("HELP.footer").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
    }

}
