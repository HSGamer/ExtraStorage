package me.hsgamer.extrastorage.commands.subs.player;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.FilterGui;

@Command(value = "filter", permission = Constants.PLAYER_FILTER_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class FilterCmd
        extends CommandListener {

    @Override
    public void execute(CommandContext context) {
        new FilterGui(context.castToPlayer(), 1).open();
    }

}
