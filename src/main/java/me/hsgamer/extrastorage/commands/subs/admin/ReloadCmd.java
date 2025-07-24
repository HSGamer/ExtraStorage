package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;

@Command(value = {"reload", "rld", "rl"}, permission = Constants.ADMIN_RELOAD_PERMISSION)
public final class ReloadCmd
        extends CommandListener {

    @Override
    public void execute(CommandContext context) {
        instance.getSetting().reload();
        instance.getMessage().reload();
        instance.getWorthManager().reload();

        context.sendMessage(Message.getMessage("SUCCESS.config-reload"));
    }

}
