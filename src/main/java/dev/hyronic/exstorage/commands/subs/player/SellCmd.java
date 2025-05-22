package dev.hyronic.exstorage.commands.subs.player;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.item.Item;
import dev.hyronic.exstorage.api.storage.Storage;
import dev.hyronic.exstorage.commands.abstraction.Command;
import dev.hyronic.exstorage.commands.abstraction.CommandContext;
import dev.hyronic.exstorage.commands.abstraction.CommandListener;
import dev.hyronic.exstorage.commands.abstraction.CommandTarget;
import dev.hyronic.exstorage.configs.Message;
import dev.hyronic.exstorage.data.Constants;
import dev.hyronic.exstorage.gui.SellGui;
import dev.hyronic.exstorage.util.Digital;
import dev.hyronic.exstorage.util.Utils;
import org.bukkit.entity.Player;

import java.util.Optional;

@Command(value = "sell", permission = Constants.PLAYER_SELL_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class SellCmd
        extends CommandListener<ExtraStorage> {

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();

        if (context.getArgsLength() == 0) {
            new SellGui(player, 1).open();
            return;
        }
        Storage storage = instance.getUserManager().getUser(player).getStorage();

        String key = context.getArgs(0);
        Optional<Item> optional = storage.getItem(key);
        if (!optional.isPresent()) {
            context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(Utils.getRegex("player"), player.getName()));
            return;
        }

        Item item = optional.get();
        int quantity = item.getQuantity();
        if (quantity < 1) {
            context.sendMessage(Message.getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(key, true)));
            return;
        }

        if (context.getArgsLength() == 1) {
            instance.getSetting()
                    .getEconomyProvider()
                    .sellItem(player, item.getItem(), quantity, result -> {
                        if (!result.isSuccess()) {
                            context.sendMessage(Message.getMessage("FAIL.cannot-be-sold"));
                            return;
                        }
                        storage.subtract(key, quantity);
                        context.sendMessage(Message.getMessage("SUCCESS.item-sold")
                                .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(quantity))
                                .replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(key, true))
                                .replaceAll(Utils.getRegex("price"), Digital.formatDouble("###,###.##", result.getPrice())));
                    });
            return;
        }

        String args1 = context.getArgs(1);
        int amount;
        try {
            amount = Digital.getBetween(1, quantity, Integer.parseInt(args1));
        } catch (NumberFormatException ignored) {
            context.sendMessage(Message.getMessage("FAIL.not-number").replaceAll(VALUE_REGEX, args1));
            return;
        }

        instance.getSetting()
                .getEconomyProvider()
                .sellItem(player, item.getItem(), amount, result -> {
                    if (!result.isSuccess()) {
                        context.sendMessage(Message.getMessage("FAIL.cannot-be-sold"));
                        return;
                    }
                    storage.subtract(key, amount);
                    context.sendMessage(Message.getMessage("SUCCESS.item-sold")
                            .replaceAll(Utils.getRegex("amount"), Digital.formatThousands(amount))
                            .replaceAll(Utils.getRegex("item"), instance.getSetting().getNameFormatted(key, true))
                            .replaceAll(Utils.getRegex("price"), Digital.formatDouble("###,###.##", result.getPrice())));
                });
    }

}
