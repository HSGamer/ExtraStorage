package dev.hyronic.exstorage.commands.subs.admin;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.commands.abstraction.CommandTarget;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.gui.WhitelistGui;

@Command(value = "whitelist", permission = Constants.ADMIN_WHITELIST_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class WhitelistCmd
        extends CommandListener<ExtraStorage> {

    @Override
    public void execute(CommandContext context) {
        new WhitelistGui(context.castToPlayer(), 1).open();
    }

}
