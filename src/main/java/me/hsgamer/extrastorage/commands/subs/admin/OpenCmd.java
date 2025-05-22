package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.StorageGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Command(value = "open", permission = Constants.ADMIN_OPEN_PERMISSION, target = CommandTarget.ONLY_PLAYER, minArgs = 1)
public final class OpenCmd
        extends CommandListener {

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
