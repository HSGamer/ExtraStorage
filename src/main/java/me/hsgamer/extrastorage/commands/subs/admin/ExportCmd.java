package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.data.Constants;

@Command(value = {"export"}, permission = Constants.ADMIN_FILE_PERMISSION)
public class ExportCmd extends CommandListener {
    @Override
    public void execute(CommandContext context) {

    }
}
