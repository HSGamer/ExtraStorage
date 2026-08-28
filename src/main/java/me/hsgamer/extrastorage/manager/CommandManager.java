package me.hsgamer.extrastorage.manager;

import io.github.projectunified.craftcommand.bukkit.BukkitCommandManager;
import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.Debug;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.command.AdminCommand;
import me.hsgamer.extrastorage.command.PlayerCommand;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;

public class CommandManager implements Loadable {
    private final ExtraStorage plugin;
    private BukkitCommandManager commandManager;

    public CommandManager(ExtraStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        commandManager = new BukkitCommandManager(plugin, (sender, e) -> {
            Debug.log("Error when executing the command", e);
            sender.sendMessage(ChatColor.RED + e.getMessage());
        });
        commandManager.registerSenderResolver(User.class, sender -> {
            if (sender instanceof Player) {
                return plugin.get(UserManager.class).getUser((Player) sender);
            }
            throw new CommandException(Utils.formatMessage(plugin.get(MessageConfig.class).fail().onlyPlayers()));
        });
        commandManager.register(new PlayerCommand(plugin));
        commandManager.register(new AdminCommand(plugin));
        commandManager.syncCommand();
    }

    @Override
    public void disable() {
        if (commandManager != null) {
            commandManager.unregisterAll();
        }
    }

    public BukkitCommandManager getCommandManager() {
        return commandManager;
    }
}
