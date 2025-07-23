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

@Command(value = "set", usage = "/{label} set <material-key> <amount> [player]", permission = Constants.ADMIN_SUBTRACT_PERMISSION, minArgs = 2)
public final class SetCmd
        extends CommandListener {

    private final Setting setting;
    private final String QUANTITY_REGEX, ITEM_REGEX, PLAYER_REGEX;

    public SetCmd() {
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
            amount = Digital.getBetween(0, Long.MAX_VALUE, Long.parseLong(args1));
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
            Item item = optional.get();

            long space = storage.getSpace();
            if (space != -1) {
                long usedSpace = storage.getUsedSpace() - item.getQuantity();
                if ((storage.getUsedSpace() == usedSpace) && storage.isMaxSpace()) {
                    context.sendMessage(Message.getMessage("FAIL.storage-is-full"));
                    return;
                }
                if ((usedSpace + amount) > space) amount = (space - usedSpace);
            }
            storage.set(args0, amount);

            context.sendMessage(Message.getMessage("SUCCESS.Set.self")
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
        Item item = optional.get();

        long space = storage.getSpace();
        if (space != -1) {
            long usedSpace = storage.getUsedSpace() - item.getQuantity();
            if ((storage.getUsedSpace() == usedSpace) && storage.isMaxSpace()) {
                context.sendMessage(Message.getMessage("FAIL.storage-is-full"));
                return;
            }
            if ((usedSpace + amount) > space) amount = (space - usedSpace);
        }
        storage.set(args0, amount);

        context.sendMessage(Message.getMessage("SUCCESS.Set.sender")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        if (player.isOnline()) player.getPlayer().sendMessage(Message.getMessage("SUCCESS.Set.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, context.getName()));
        else user.save();
    }

}
