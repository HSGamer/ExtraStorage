package dev.hyronic.exstorage.commands.subs.admin;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.storage.Storage;
import dev.hyronic.exstorage.api.user.User;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.data.user.UserManager;
import dev.hyronic.exstorage.util.Digital;
import dev.hyronic.exstorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@Command(value = "addspace", usage = "/{label} addspace <amount> [player|*]", permission = Constants.ADMIN_SPACE_PERMISSION, minArgs = 1)
public final class AddSpaceCmd
        extends CommandListener<ExtraStorage> {

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
            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
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
