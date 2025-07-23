package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Command(value = "add", usage = "/{label} add <material-key> <amount> [player]", permission = Constants.ADMIN_ADD_PERMISSION, minArgs = 2)
public final class AddCmd
        extends CommandListener {

    private final Setting setting;
    private final String QUANTITY_REGEX, ITEM_REGEX, PLAYER_REGEX;

    public AddCmd() {
        this.setting = instance.getSetting();

        this.QUANTITY_REGEX = Utils.getRegex("quantity");
        this.ITEM_REGEX = Utils.getRegex("item");
        this.PLAYER_REGEX = Utils.getRegex("player");
    }

    @Override
    public void execute(CommandContext context) {
        String args0 = context.getArgs(0), args1 = context.getArgs(1);
        long amount;
        try {
            amount = Digital.getBetween(1, Long.MAX_VALUE, Long.parseLong(args1));
        } catch (NumberFormatException ignored) {
            context.sendMessage(Message.getMessage("FAIL.not-number").replaceAll(VALUE_REGEX, args1));
            return;
        }

        if (context.getArgsLength() == 2) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.getMessage("FAIL.must-enter-player"));
                return;
            }
            Player player = context.castToPlayer();
            Storage storage = instance.getUserManager().getUser(player).getStorage();

            Optional<Item> optional = storage.getItem(args0);
            if (!optional.isPresent()) {
                context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
                return;
            }

            long freeSpace = storage.getFreeSpace();
            if (freeSpace != -1) {
                if (freeSpace < 1) {
                    context.sendMessage(Message.getMessage("FAIL.storage-is-full"));
                    return;
                }
                if (amount > freeSpace) amount = freeSpace;
            }
            storage.add(args0, amount);

            context.sendMessage(Message.getMessage("SUCCESS.Add.self")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true)));
            return;
        }

        String args2 = context.getArgs(2);
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(args2);
        User user = instance.getUserManager().getUser(player);
        if (user == null) {
            context.sendMessage(Message.getMessage("FAIL.player-not-found"));
            return;
        }
        Storage storage = user.getStorage();

        Optional<Item> optional = storage.getItem(args0);
        if (!optional.isPresent()) {
            context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
            return;
        }

        long freeSpace = storage.getFreeSpace();
        if (freeSpace != -1) {
            if (freeSpace < 1) {
                context.sendMessage(Message.getMessage("FAIL.storage-is-full"));
                return;
            }
            if (amount > freeSpace) amount = freeSpace;
        }
        storage.add(args0, amount);

        context.sendMessage(Message.getMessage("SUCCESS.Add.sender")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        if (player.isOnline()) player.getPlayer().sendMessage(Message.getMessage("SUCCESS.Add.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, context.getName()));
        else user.save();
    }

}
