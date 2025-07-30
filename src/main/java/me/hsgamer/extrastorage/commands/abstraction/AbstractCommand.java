package me.hsgamer.extrastorage.commands.abstraction;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCommand
        extends CommandBase
        implements CommandExecutor {

    private final Map<String, CommandListener> listeners;

    public AbstractCommand() {
        this.listeners = new HashMap<>();
    }

    public void addPrimaryCommand(CommandListener listener) {
        if (!listener.getClass().isAnnotationPresent(Command.class)) return;
        Command cmd = listener.getClass().getAnnotation(Command.class);
        listeners.put(cmd.value()[0], listener);

        instance.getCommand(cmd.value()[0]).setExecutor(this);
        instance.getCommand(cmd.value()[0]).setTabCompleter(listener);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        CommandListener listener = listeners.get(command.getLabel());

        while (args.length > 0) {
            CommandListener subCmd = listener.getCommand(args[0]);
            if (subCmd == null) break;
            listener = subCmd;
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        if (!this.check(sender, listener.getClass().getAnnotation(Command.class), label, args)) return true;

        listener.execute(new CommandContext(sender, label, args));
        return true;
    }

    public abstract boolean check(CommandSender sender, Command cmd, String label, String[] args);

}
