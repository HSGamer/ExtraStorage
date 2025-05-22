package dev.hyronic.exstorage.commands.subs.admin;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.user.User;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.commands.abstraction.CommandTarget;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.gui.StorageGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Command(value = "open", permission = Constants.ADMIN_OPEN_PERMISSION, target = CommandTarget.ONLY_PLAYER, minArgs = 1)
public final class OpenCmd
        extends CommandListener<ExtraStorage> {

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();

        String args0 = context.getArgs(0);
        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(args0);
        User user = instance.getUserManager().getUser(target);
        if (user == null) {
            context.sendMessage(Message.getMessage("FAIL.player-not-found"));
            return;
        }

        new StorageGui(player, user, 1).open();
    }

}
