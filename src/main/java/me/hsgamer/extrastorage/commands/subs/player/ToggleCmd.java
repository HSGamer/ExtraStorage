package me.hsgamer.extrastorage.commands.subs.player;

import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.entity.Player;

@Command(value = "toggle", permission = Constants.PLAYER_TOGGLE_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class ToggleCmd
        extends CommandListener {

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();

        Storage storage = instance.getUserManager().getUser(player).getStorage();
        boolean toggled = !storage.getStatus();
        storage.setStatus(toggled);

        context.sendMessage(Message.getMessage("SUCCESS.storage-usage-toggled")
                .replaceAll(Utils.getRegex("status"), Message.getMessage("STATUS." + (toggled ? "enabled" : "disabled"))));
    }

}
