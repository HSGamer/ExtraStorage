package dev.hyronic.exstorage.commands.subs.player;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.storage.Storage;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.commands.abstraction.CommandTarget;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.util.Utils;
import org.bukkit.entity.Player;

@Command(value = "toggle", permission = Constants.PLAYER_TOGGLE_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class ToggleCmd
        extends CommandListener<ExtraStorage> {

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
