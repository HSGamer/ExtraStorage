package dev.hyronic.exstorage.commands;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.item.Item;
import dev.hyronic.exstorage.api.user.User;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.commands.abstraction.CommandTarget;
import dev.hyronic.exstorage.commands.subs.player.*;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.gui.StorageGui;
import dev.hyronic.exstorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(value = "extrastorage", permission = Constants.PLAYER_OPEN_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class PlayerCommands
        extends CommandListener<ExtraStorage> {

    public PlayerCommands() {
        this.add(new HelpCmd());
        this.add(new ToggleCmd());
        this.add(new FilterCmd());
        this.add(new PartnerCmd());
        this.add(new SellCmd());
        this.add(new WithdrawCmd());
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();

        if (context.getArgsLength() == 0) {
            new StorageGui(player, 1).open();
            return;
        }

        String args0 = context.getArgs(0);
        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(args0);
        User user = instance.getUserManager().getUser(target);
        if (user == null) {
            context.sendMessage(Message.getMessage("FAIL.player-not-found"));
            return;
        }

        if (target.getName().equals(player.getName())) {
            context.sendMessage(Message.getMessage("FAIL.not-yourself"));
            return;
        }

        if (!user.isPartner(player.getUniqueId())) {
            context.sendMessage(Message.getMessage("FAIL.player-not-partner").replaceAll(Utils.getRegex("player"), target.getName()));
            return;
        }

        new StorageGui(player, user, 1).open();
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        List<String> cmds = Arrays.asList(
                this.hasPermission(sender, Constants.PLAYER_HELP_PERMISSION) ? "help" : "",
                this.hasPermission(sender, Constants.PLAYER_TOGGLE_PERMISSION) ? "toggle" : "",
                this.hasPermission(sender, Constants.PLAYER_FILTER_PERMISSION) ? "filter" : "",
                this.hasPermission(sender, Constants.PLAYER_PARTNER_PERMISSION) ? "partner" : "",
                this.hasPermission(sender, Constants.PLAYER_SELL_PERMISSION) ? "sell" : "",
                this.hasPermission(sender, Constants.PLAYER_WITHDRAW_PERMISSION) ? "withdraw" : ""
        );

        String args0 = args[0].toLowerCase();
        if (args.length == 1) return cmds.stream().filter(cmd -> cmd.startsWith(args0)).collect(Collectors.toList());

        User user = instance.getUserManager().getUser(player);

        String args1 = args[1].toLowerCase();
        if (args.length == 2) {
            switch (args0) {
                case "partner":
                    return Stream.of("add", "remove", "clear")
                            .filter(cmd -> cmd.startsWith(args1))
                            .collect(Collectors.toList());
                case "withdraw":
                case "sell":
                    return user.getStorage()
                            .getItems()
                            .values()
                            .stream()
                            .filter(item -> (item.getKey().toLowerCase().startsWith(args1)) && (item.getQuantity() > 0))
                            .map(Item::getKey)
                            .collect(Collectors.toList());
                default:
                    return null;
            }
        }

        return null;
    }

}
