package me.hsgamer.extrastorage.commands;

import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.subs.admin.*;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Command(value = "esadmin", permission = Constants.ADMIN_HELP_PERMISSION)
public final class AdminCommands
        extends CommandListener {

    public AdminCommands() {
        this.add(new OpenCmd());
        this.add(new SpaceCmd());
        this.add(new AddSpaceCmd());
        this.add(new AddRndCmd());
        this.add(new AddCmd());
        this.add(new SubtractCmd());
        this.add(new SetCmd());
        this.add(new ResetCmd());
        this.add(new WhitelistCmd());
        this.add(new ReloadCmd());
    }

    @Override
    public void execute(CommandContext context) {
        context.sendMessage(Message.getMessage("HELP.header").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
        context.sendMessage(Message.getMessage("HELP.Admin.help").replaceAll(LABEL_REGEX, context.getLabel()));
        if (context.hasPermission(Constants.ADMIN_OPEN_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.open").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_SPACE_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.space").replaceAll(LABEL_REGEX, context.getLabel()));
            context.sendMessage(Message.getMessage("HELP.Admin.addspace").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_ADD_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.add").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_SUBTRACT_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.subtract").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_SET_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.set").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_RESET_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.reset").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_WHITELIST_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.whitelist").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        if (context.hasPermission(Constants.ADMIN_RELOAD_PERMISSION)) {
            context.sendMessage(Message.getMessage("HELP.Admin.reload").replaceAll(LABEL_REGEX, context.getLabel()));
        }
        context.sendMessage(Message.getMessage("HELP.footer").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        List<String> cmds = Arrays.asList(
                this.hasPermission(player, Constants.ADMIN_HELP_PERMISSION) ? "help" : "",
                this.hasPermission(player, Constants.ADMIN_OPEN_PERMISSION) ? "open" : "",
                this.hasPermission(player, Constants.ADMIN_SPACE_PERMISSION) ? "space" : "",
                this.hasPermission(player, Constants.ADMIN_SPACE_PERMISSION) ? "addspace" : "",
                this.hasPermission(player, Constants.ADMIN_ADD_PERMISSION) ? "add" : "",
                this.hasPermission(player, Constants.ADMIN_SUBTRACT_PERMISSION) ? "subtract" : "",
                this.hasPermission(player, Constants.ADMIN_SET_PERMISSION) ? "set" : "",
                this.hasPermission(player, Constants.ADMIN_RESET_PERMISSION) ? "reset" : "",
                this.hasPermission(player, Constants.ADMIN_WHITELIST_PERMISSION) ? "whitelist" : "",
                this.hasPermission(player, Constants.ADMIN_RELOAD_PERMISSION) ? "reload" : ""
        );

        String args0 = args[0].toLowerCase();
        if (args.length == 1) return cmds.stream().filter(cmd -> cmd.startsWith(args0)).collect(Collectors.toList());

        User user = instance.getUserManager().getUser(player);

        String args1 = args[1].toLowerCase();
        if (args.length == 2) {
            List<String> list = user.getStorage()
                    .getItems()
                    .keySet()
                    .stream()
                    .filter(key -> key.toLowerCase().startsWith(args1))
                    .collect(Collectors.toList());
            switch (args0) {
                case "add":
                case "subtract":
                case "set":
                    return list;
                case "addrnd":
                case "reset":
                    list.add("*");
                    return list;
            }
            return null;
        }

        return null;
    }

}
