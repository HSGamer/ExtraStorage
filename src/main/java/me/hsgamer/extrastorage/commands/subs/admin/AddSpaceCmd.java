package me.hsgamer.extrastorage.commands.subs.admin;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Command(value = "addspace", usage = "/{label} addspace <amount> [player|*]", permission = Constants.ADMIN_SPACE_PERMISSION, minArgs = 1)
public final class AddSpaceCmd
        extends CommandListener {

    private final UserManager manager;

    public AddSpaceCmd() {
        this.manager = instance.getUserManager();
    }

    @Override
    public void execute(CommandContext context) {
        if (instance.getSetting().getMaxSpace() == -1) {
            context.sendMessage(Message.getMessage("FAIL.max-space-not-used"));
            return;
        }

        String args0 = context.getArgs(0);
        long space;
        try {
            space = Digital.getBetween(1, Long.MAX_VALUE, Long.parseLong(args0));
        } catch (NumberFormatException ignored) {
            context.sendMessage(Message.getMessage("FAIL.not-number").replaceAll(VALUE_REGEX, args0));
            return;
        }

        if (context.getArgsLength() == 1) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.getMessage("FAIL.must-enter-player"));
                return;
            }
            User user = manager.getUser(context.castToPlayer());
            Storage storage = user.getStorage();

            if (this.checkIntLimit(storage.getSpace(), space)) {
                context.sendMessage(Message.getMessage("FAIL.space-exceed"));
                return;
            }

            storage.addSpace(space);
            context.sendMessage(Message.getMessage("SUCCESS.space-increased").replaceAll(Utils.getRegex("space"), Digital.formatThousands(space)));
            return;
        }

        String args1 = context.getArgs(1);
        if (args1.matches("(?ium)(\\*|-all)")) {
            AsyncScheduler.get(instance).run(() -> {
                for (User user : manager.getUsers()) {
                    Storage storage = user.getStorage();

                    if (this.checkIntLimit(storage.getSpace(), space)) continue;
                    storage.addSpace(space);

                    Player player = user.getPlayer();
                    if ((player != null) && player.isOnline())
                        player.sendMessage(Message.getMessage("SUCCESS.space-increased").replaceAll(Utils.getRegex("space"), Digital.formatThousands(space)));
                    else user.save();
                }
            });
            return;
        }

        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args1);
        User user = manager.getUser(player);
        if (user == null) {
            context.sendMessage(Message.getMessage("FAIL.player-not-found"));
            return;
        }
        Storage storage = user.getStorage();

        if (this.checkIntLimit(storage.getSpace(), space)) {
            context.sendMessage(Message.getMessage("FAIL.space-exceed"));
            return;
        }
        storage.addSpace(space);

        context.sendMessage(Message.getMessage("SUCCESS.space-increased").replaceAll(Utils.getRegex("space"), Digital.formatThousands(space)));
        if (player.isOnline())
            player.getPlayer().sendMessage(Message.getMessage("SUCCESS.space-increased").replaceAll(Utils.getRegex("space"), Digital.formatThousands(space)));
        else user.save();
    }

    private boolean checkIntLimit(long value, long increValue) {
        return (Long.MAX_VALUE - increValue) < value;
    }

}
