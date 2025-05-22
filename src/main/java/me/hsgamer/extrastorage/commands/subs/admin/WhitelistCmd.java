package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.WhitelistGui;

@Command(value = "whitelist", permission = Constants.ADMIN_WHITELIST_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class WhitelistCmd
        extends CommandListener {

    @Override
    public void execute(CommandContext context) {
        new WhitelistGui(context.castToPlayer(), 1).open();
    }

}
