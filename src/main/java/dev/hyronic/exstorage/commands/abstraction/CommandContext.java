package dev.hyronic.exstorage.commands.abstraction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class CommandContext {

    private final CommandSender sender;
    private final String label;
    private final String[] arguments;

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

}
