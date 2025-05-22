package dev.hyronic.exstorage.commands.subs.player;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;

@Command(value = {"help", "?"}, permission = Constants.PLAYER_HELP_PERMISSION)
public final class HelpCmd
        extends CommandListener<ExtraStorage> {

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
