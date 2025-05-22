package me.hsgamer.extrastorage.commands.abstraction;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CommandListener
        extends CommandBase
        implements TabCompleter {

    private final List<CommandListener> listeners;

    public CommandListener() {
        this.listeners = new ArrayList<>();
    }

    protected void add(CommandListener listener) {
        if (!listener.getClass().isAnnotationPresent(Command.class)) return;
        listeners.add(listener);
    }

    public CommandListener getCommand(String command) {
        return listeners.stream()
                .filter(lis -> Arrays.stream(lis.getClass().getAnnotation(Command.class).value()).anyMatch(command::equalsIgnoreCase))
                .findFirst()
                .orElse(null);
    }

    public abstract void execute(CommandContext context);

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        return null;
    }

}
