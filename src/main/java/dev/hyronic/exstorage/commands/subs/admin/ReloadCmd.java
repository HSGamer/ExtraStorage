package dev.hyronic.exstorage.commands.subs.admin;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;

@Command(value = {"reload", "rld", "rl"}, permission = Constants.ADMIN_RELOAD_PERMISSION)
public final class ReloadCmd
        extends CommandListener<ExtraStorage> {

    @Override
    public void execute(CommandContext context) {
        instance.getSetting().reload();
        instance.getMessage().reload();
        instance.getWorthManager().reload();

        instance.getAutoUpdateTask().setTime(instance.getSetting().getAutoUpdateTime());
        if (instance.getSetting().isRestartOnChange()) instance.getAutoUpdateTask().resetTime();

        context.sendMessage(Message.getMessage("SUCCESS.config-reload"));
    }

}
