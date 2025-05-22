package me.hsgamer.extrastorage.commands.subs.player;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@Command(value = "withdraw", usage = "/{label} withdraw <material-key> [amount]", permission = Constants.PLAYER_WITHDRAW_PERMISSION, target = CommandTarget.ONLY_PLAYER, minArgs = 1)
public final class WithdrawCmd
        extends CommandListener {

    private final Setting setting;

    public WithdrawCmd() {
        this.setting = instance.getSetting();
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();
        Storage storage = instance.getUserManager().getUser(player).getStorage();

        String args0 = context.getArgs(0);
        Optional<Item> optional = storage.getItem(args0);
        if (!optional.isPresent()) {
            context.sendMessage(Message.getMessage("FAIL.item-not-in-storage").replaceAll(Utils.getRegex("player"), player.getName()));
            return;
        }
        Item item = optional.get();

        int current = item.getQuantity();
        if (current < 1) {
            context.sendMessage(Message.getMessage("FAIL.not-enough-item").replaceAll(Utils.getRegex("item"), setting.getNameFormatted(args0, true)));
            return;
        }
        ItemStack iStack = item.getItem().clone();

        if (context.getArgsLength() == 1) iStack.setAmount(current);
        else {
            String args1 = context.getArgs(1);
            int amount;
            try {
                amount = Digital.getBetween(1, current, Integer.parseInt(args1));
            } catch (NumberFormatException ignored) {
                context.sendMessage(Message.getMessage("FAIL.not-number").replaceAll(VALUE_REGEX, args1));
                return;
            }
            iStack.setAmount(amount);
        }
        if (item.getType() == ItemUtil.ItemType.VANILLA) {
            /*
             * Cần xoá Meta của Item khi rút vì xảy ra trường hợp sau khi rút xong
             * thì item sẽ có Meta, khiến cho việc drop ra mặt đất và không thể
             * nhặt lại vào kho chứa được.
             * Việc setItemMeta(null) sẽ không bị lỗi ở bất kỳ phiên bản nào.
             */
            iStack.setItemMeta(null);
        }

        int free = this.getFreeSpace(player, iStack);
        if (free == -1) {
            // Nếu kho đồ đã đầy:
            player.sendMessage(Message.getMessage("FAIL.inventory-is-full"));
            return;
        }
        iStack.setAmount(free);

        storage.subtract(args0, free);
        ItemUtil.giveItem(player, iStack);

        context.sendMessage(Message.getMessage("SUCCESS.withdrew-item")
                .replaceAll(Utils.getRegex("quantity"), Digital.formatThousands(free))
                .replaceAll(Utils.getRegex("item"), setting.getNameFormatted(args0, true)));
    }

    /*
     * Trả về khoảng trống còn lại trong kho đồ của người chơi:
     * Sẽ là -1 nếu không còn khoảng trống nào.
     */
    private int getFreeSpace(Player player, ItemStack item) {
        ItemStack[] items = player.getInventory().getStorageContents();
        int empty = 0;
        for (ItemStack stack : items) {
            if ((stack == null) || (stack.getType() == Material.AIR)) {
                empty += item.getMaxStackSize();
                continue;
            }
            if (!item.isSimilar(stack)) continue;
            empty += (stack.getMaxStackSize() - stack.getAmount());
        }
        if (empty > 0) return Math.min(empty, item.getAmount());
        return -1;
    }

}
