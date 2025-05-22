package dev.hyronic.exstorage.commands.subs.admin;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.item.Item;
import dev.hyronic.exstorage.api.storage.Storage;
import dev.hyronic.exstorage.api.user.User;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.configs.Setting;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Command(value = "reset", usage = "/{label} reset <material-key|*> [player]", permission = Constants.ADMIN_RESET_PERMISSION, minArgs = 1)
public final class ResetCmd
        extends CommandListener<ExtraStorage> {

    private final Setting setting;
    private final String ITEM_REGEX, PLAYER_REGEX;

    public ResetCmd() {
        this.setting = instance.getSetting();

        this.ITEM_REGEX = Utils.getRegex("item");
        this.PLAYER_REGEX = Utils.getRegex("player");
    }

    @Override
    public void execute(CommandContext context) {
        String args0 = context.getArgs(0);
        boolean isAll = args0.matches("(?ium)(-all|\\*)");

        if (context.getArgsLength() == 1) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.getMessage("FAIL.must-enter-player"));
                return;
            }
            Player player = context.castToPlayer();
            Storage storage = instance.getUserManager().getUser(player).getStorage();

            if (isAll) {
                storage.reset(null);
                context.sendMessage(Message.getMessage("SUCCESS.Reset.all"));
                return;
            }

            Optional<Item> optional = storage.getItem(args0);
            if (!optional.isPresent()) {
                context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
                return;
            }
            storage.reset(args0);

            context.sendMessage(Message.getMessage("SUCCESS.Reset.self").replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true)));
            return;
        }

        String args2 = context.getArgs(1);
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args2);
        User user = instance.getUserManager().getUser(player);
        if (user == null) {
            context.sendMessage(Message.getMessage("FAIL.player-not-found"));
            return;
        }
        Storage storage = user.getStorage();

        if (isAll) {
            storage.reset(null);
            context.sendMessage(Message.getMessage("SUCCESS.Reset.all-sender").replaceAll(PLAYER_REGEX, player.getName()));
            if (player.isOnline()) player.getPlayer().sendMessage(Message.getMessage("SUCCESS.Reset.all"));
            return;
        }

        Optional<Item> optional = storage.getItem(args0);
        if (!optional.isPresent()) {
            context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
            return;
        }
        storage.reset(args0);

        context.sendMessage(Message.getMessage("SUCCESS.Reset.sender")
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        if (player.isOnline()) player.getPlayer().sendMessage(Message.getMessage("SUCCESS.Reset.target")
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, context.getName()));
        else user.save();
    }

}
