package me.hsgamer.extrastorage.commands.subs.admin;

import me.hsgamer.extrastorage.api.item.Item;
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
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Command(value = "addrnd", usage = "/{label} addrnd <material-key|*> [player]", permission = Constants.ADMIN_ADD_PERMISSION, minArgs = 1)
public final class AddRndCmd
        extends CommandListener {

    private final UserManager manager;

    private final String PLAYER_REGEX, QUANTITY_REGEX, ITEM_REGEX;
    private final int MIN, MAX;

    public AddRndCmd() {
        this.manager = instance.getUserManager();

        this.PLAYER_REGEX = Utils.getRegex("player");
        this.QUANTITY_REGEX = Utils.getRegex("quantity");
        this.ITEM_REGEX = Utils.getRegex("item");

        this.MIN = 1000;
        this.MAX = 100000;
    }

    @Override
    public void execute(CommandContext context) {
        String args0 = context.getArgs(0);
        if (context.getArgsLength() == 1) {
            if (!context.isPlayer()) {
                context.sendMessage(Message.getMessage("FAIL.must-enter-player"));
                return;
            }
            Player player = context.castToPlayer();
            Storage storage = manager.getUser(player).getStorage();

            if (args0.equals("*")) {
                String[] keys = storage.getItems().keySet().toArray(new String[0]);
                long total = this.addQuantity(storage, keys);
                if (total < 1) {
                    context.sendMessage(ChatColor.RED + "Failed to add random quantity to your storage! May be your storage is full or something goes wrong!");
                    return;
                }
                context.sendMessage(Message.getMessage("SUCCESS.Add.self")
                        .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                        .replaceAll(ITEM_REGEX, "all"));
                return;
            }

            Optional<Item> optional = storage.getItem(args0);
            if (!optional.isPresent()) {
                context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
                return;
            }

            long amount = Digital.random(MIN, MAX);
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
                    .replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(args0, true)));
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

        if (args0.equals("*")) {
            String[] keys = storage.getItems().keySet().toArray(new String[0]);
            long total = this.addQuantity(storage, keys);
            if (total < 1) {
                context.sendMessage(ChatColor.RED + "Failed to add random quantity to your storage! May be the storage is full or something goes wrong!");
                return;
            }
            context.sendMessage(Message.getMessage("SUCCESS.Add.sender")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                    .replaceAll(ITEM_REGEX, "all")
                    .replaceAll(PLAYER_REGEX, player.getName()));
            if (player.isOnline()) player.getPlayer().sendMessage(Message.getMessage("SUCCESS.Add.target")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                    .replaceAll(ITEM_REGEX, "all")
                    .replaceAll(PLAYER_REGEX, context.getName()));
            else user.save();
            return;
        }

        Optional<Item> optional = storage.getItem(args0);
        if (!optional.isPresent()) {
            context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
            return;
        }

        long amount = Digital.random(MIN, MAX);
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
                .replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        if (player.isOnline()) player.getPlayer().sendMessage(Message.getMessage("SUCCESS.Add.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(args0, true))
                .replaceAll(PLAYER_REGEX, context.getName()));
        else user.save();
    }

    private long addQuantity(Storage storage, String... keys) {
        if ((keys == null) || (keys.length < 1)) return -1;

        long count = 0;
        for (String key : keys) {
            Optional<Item> optional = storage.getItem(key);
            if (!optional.isPresent()) continue;

            long amount = Digital.random(MIN, MAX);
            long freeSpace = storage.getFreeSpace();

            if (freeSpace != -1) {
                if (freeSpace < 1) break;
                if (amount > freeSpace) amount = freeSpace;
            }

            storage.add(key, amount);
            count += amount;

            if (amount == freeSpace) break;
        }

        return ((count < 1) ? -1 : count);
    }

}
