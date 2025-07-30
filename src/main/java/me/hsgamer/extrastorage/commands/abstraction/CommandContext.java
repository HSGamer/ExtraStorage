package me.hsgamer.extrastorage.commands.abstraction;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandContext {

    private final CommandSender sender;
    private final String label;
    private final String[] arguments;

    CommandContext(CommandSender sender, String label, String[] arguments) {
        this.sender = sender;
        this.label = label;
        this.arguments = arguments;
    }

    public String getName() {
        return sender.getName();
    }

    public boolean isPlayer() {
        return (sender instanceof Player);
    }

    public int getArgsLength() {
        return arguments.length;
    }

    public String getArgs(int i) {
        return arguments[i];
    }

    public Player castToPlayer() {
        return (Player) sender;
    }

    public void sendMessage(String msg) {
        sender.sendMessage(msg);
    }

    public boolean hasPermission(String perm) {
        return (sender.isOp() || sender.hasPermission(perm));
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public String getLabel() {
        return this.label;
    }

    public String[] getArguments() {
        return this.arguments;
    }
}
