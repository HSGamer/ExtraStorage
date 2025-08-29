package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.data.Constants;

@Command(value = {"import"}, permission = Constants.ADMIN_FILE_PERMISSION)
public class ImportCmd extends CommandListener {
    @Override
    public void execute(CommandContext context) {

    }
}
