package dev.hyronic.exstorage.commands.subs.player;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.commands.abstraction.CommandTarget;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.gui.FilterGui;

@Command(value = "filter", permission = Constants.PLAYER_FILTER_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class FilterCmd
        extends CommandListener<ExtraStorage> {

    @Override
    public void execute(CommandContext context) {
        new FilterGui(context.castToPlayer(), 1).open();
    }

}
