package me.hsgamer.extrastorage.commands;

import io.github.projectunified.craftcommand.annotation.*;
import io.github.projectunified.craftcommand.bukkit.annotation.Permission;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.MessageConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.FilterGUI;
import me.hsgamer.extrastorage.gui.PartnerGUI;
import me.hsgamer.extrastorage.gui.SellGUI;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

@Command(value = "extrastorage", aliases = {"exstorage", "storage", "es", "kho"}, description = "Commands for players")
public class PlayerCommand {

    private final ExtraStorage instance = ExtraStorage.getInstance();

    private static final String VERSION_REGEX = Utils.getRegex("ver(sion)?");
    private static final String LABEL_REGEX = Utils.getRegex("label");
    private static final String PLAYER_REGEX = Utils.getRegex("player");
    private static final String ITEM_REGEX = Utils.getRegex("item");
    private static final String AMOUNT_REGEX = Utils.getRegex("amount");
    private static final String QUANTITY_REGEX = Utils.getRegex("quantity");
    private static final String PRICE_REGEX = Utils.getRegex("price");
    private static final String VALUE_REGEX = Utils.getRegex("value");
    private static final String STATUS_REGEX = Utils.getRegex("status");

    public User resolveUser(CommandSender sender) {
        if (sender instanceof Player) {
            return instance.getUserManager().getUser((Player) sender);
        }
        throw new CommandException(instance.getMessage().getMessage("FAIL.only-players"));
    }

    @Default
    @Permission(Constants.PLAYER_OPEN_PERMISSION)
    public void execute(@Resolve("resolveUser") User user, @Default String target) {
        Player player = user.getPlayer();

        if (target == null) {
            new StorageGUI(player, null).open();
            return;
        }

        OfflinePlayer targetPlayer = Bukkit.getServer().getOfflinePlayer(target);
        if (!targetPlayer.hasPlayedBefore()) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.player-not-found"));
        }
        User targetUser = instance.getUserManager().getUser(targetPlayer);

        if (target.equals(player.getName())) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.not-yourself"));
        }

        if (!targetUser.isPartner(player.getUniqueId())) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.player-not-partner").replaceAll(PLAYER_REGEX, target));
        }

        new StorageGUI(player, targetUser).open();
    }

    @Command(value = "help", aliases = {"?"})
    @Permission(Constants.PLAYER_HELP_PERMISSION)
    public void help(CommandSender sender) {
        sender.sendMessage(instance.getMessage().getMessage("HELP.header").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
        sender.sendMessage(instance.getMessage().getMessage("HELP.Player.help").replaceAll(LABEL_REGEX, "extrastorage"));
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) {
            sender.sendMessage(instance.getMessage().getMessage("HELP.Player.open").replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) {
            sender.sendMessage(instance.getMessage().getMessage("HELP.Player.toggle").replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) {
            sender.sendMessage(instance.getMessage().getMessage("HELP.Player.filter").replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) {
            sender.sendMessage(instance.getMessage().getMessage("HELP.Player.partner").replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_SELL_PERMISSION)) {
            sender.sendMessage(instance.getMessage().getMessage("HELP.Player.sell").replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_WITHDRAW_PERMISSION)) {
            sender.sendMessage(instance.getMessage().getMessage("HELP.Player.withdraw").replaceAll(LABEL_REGEX, "extrastorage"));
        }
        sender.sendMessage(instance.getMessage().getMessage("HELP.footer").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
    }

    @Command("toggle")
    @Permission(Constants.PLAYER_TOGGLE_PERMISSION)
    public void toggle(@Resolve("resolveUser") User user) {
        Storage storage = user.getStorage();
        boolean toggled = !storage.getStatus();
        storage.setStatus(toggled);

        user.getPlayer().sendMessage(instance.getMessage().getMessage("SUCCESS.storage-usage-toggled")
                .replaceAll(STATUS_REGEX, instance.getMessage().getMessage("STATUS." + (toggled ? "enabled" : "disabled"))));
    }

    @Command("filter")
    @Permission(Constants.PLAYER_FILTER_PERMISSION)
    public void filter(Player sender) {
        new FilterGUI(sender).open();
    }

    @Command("partner")
    @Permission(Constants.PLAYER_PARTNER_PERMISSION)
    public class PartnerCommand {

        public User resolveUser(CommandSender sender) {
            return PlayerCommand.this.resolveUser(sender);
        }

        @Default
        public void execute(@Resolve("resolveUser") User user) {
            new PartnerGUI(user.getPlayer()).open();
        }

        @Command("add")
        public void add(@Resolve("resolveUser") User user, String targetName) {
            Player player = user.getPlayer();

            OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.player-not-found"));
            }
            if (targetName.equals(player.getName())) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.not-yourself"));
            }
            if (user.isPartner(target.getUniqueId())) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.already-partner"));
            }

            user.addPartner(target.getUniqueId());
            player.sendMessage(instance.getMessage().getMessage("SUCCESS.made-partner").replaceAll(PLAYER_REGEX, target.getName()));
            if (target.isOnline()) {
                target.getPlayer().sendMessage(instance.getMessage().getMessage("SUCCESS.being-partner")
                        .replaceAll(PLAYER_REGEX, player.getName())
                        .replaceAll(LABEL_REGEX, "extrastorage"));
            }
        }

        @Command("remove")
        public void remove(@Resolve("resolveUser") User user, String targetName) {
            Player player = user.getPlayer();

            OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.player-not-found"));
            }
            if (targetName.equals(player.getName())) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.not-yourself"));
            }
            if (!user.isPartner(target.getUniqueId())) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.not-partner"));
            }

            user.removePartner(target.getUniqueId());
            player.sendMessage(instance.getMessage().getMessage("SUCCESS.removed-partner").replaceAll(PLAYER_REGEX, target.getName()));
            if (target.isOnline()) {
                Player p = target.getPlayer();
                p.sendMessage(instance.getMessage().getMessage("SUCCESS.no-longer-partner").replaceAll(PLAYER_REGEX, player.getName()));
                InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof StorageGUI) {
                    StorageGUI gui = (StorageGUI) holder;
                    if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                }
            }
        }

        @Command("clear")
        public void clear(@Resolve("resolveUser") User user) {
            Player player = user.getPlayer();
            Collection<Partner> partners = user.getPartners();
            if (partners.isEmpty()) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.partners-list-empty"));
            }
            for (Partner pn : partners) {
                OfflinePlayer offPlayer = pn.getOfflinePlayer();
                if (!offPlayer.isOnline()) continue;

                Player p = offPlayer.getPlayer();
                p.sendMessage(instance.getMessage().getMessage("SUCCESS.no-longer-partner").replaceAll(PLAYER_REGEX, player.getName()));
                InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                if (holder instanceof StorageGUI) {
                    StorageGUI gui = (StorageGUI) holder;
                    if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                }
            }
            user.clearPartners();
            player.sendMessage(instance.getMessage().getMessage("SUCCESS.cleanup-partners-list"));
        }
    }

    @Command("sell")
    @Permission(Constants.PLAYER_SELL_PERMISSION)
    public void sell(@Resolve("resolveUser") User user, @Default String materialKey, @Default String amountStr) {
        Player player = user.getPlayer();

        if (materialKey == null) {
            new SellGUI(player).open();
            return;
        }

        Storage storage = user.getStorage();
        Optional<Item> optional = storage.getItem(materialKey);
        if (!optional.isPresent()) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
        }

        Item item = optional.get();
        int quantity = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
        if (quantity < 1) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.not-enough-item").replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(materialKey, true)));
        }

        if (amountStr == null) {
            instance.getSetting()
                    .resolveEconomyProvider()
                    .sellItem(player, item.getItem(), quantity, result -> {
                        if (!result.isSuccess()) {
                            throw new CommandException(instance.getMessage().getMessage("FAIL.cannot-be-sold"));
                        }
                        storage.subtract(materialKey, quantity);
                        player.sendMessage(instance.getMessage().getMessage("SUCCESS.item-sold")
                                .replaceAll(AMOUNT_REGEX, Digital.formatThousands(quantity))
                                .replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(materialKey, true))
                                .replaceAll(PRICE_REGEX, Digital.formatDouble("###,###.##", result.getPrice())));
                    });
            return;
        }

        int amount;
        try {
            amount = Digital.getBetween(1, quantity, Integer.parseInt(amountStr));
        } catch (NumberFormatException ignored) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.not-number").replaceAll(VALUE_REGEX, amountStr));
        }

        instance.getSetting()
                .resolveEconomyProvider()
                .sellItem(player, item.getItem(), amount, result -> {
                    if (!result.isSuccess()) {
                        throw new CommandException(instance.getMessage().getMessage("FAIL.cannot-be-sold"));
                    }
                    storage.subtract(materialKey, amount);
                    player.sendMessage(instance.getMessage().getMessage("SUCCESS.item-sold")
                            .replaceAll(AMOUNT_REGEX, Digital.formatThousands(amount))
                            .replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(materialKey, true))
                            .replaceAll(PRICE_REGEX, Digital.formatDouble("###,###.##", result.getPrice())));
                });
    }

    @Command("withdraw")
    @Permission(Constants.PLAYER_WITHDRAW_PERMISSION)
    public void withdraw(@Resolve("resolveUser") User user, String materialKey, @Default String amountStr) {
        Player player = user.getPlayer();
        Storage storage = user.getStorage();

        Optional<Item> optional = storage.getItem(materialKey);
        if (!optional.isPresent()) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
        }
        Item item = optional.get();
        if (!item.isLoaded()) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
        }

        int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
        if (current < 1) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.not-enough-item").replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(materialKey, true)));
        }
        ItemStack iStack = item.getItem().clone();

        if (amountStr == null) {
            iStack.setAmount(current);
        } else {
            int amount;
            try {
                amount = Digital.getBetween(1, current, Integer.parseInt(amountStr));
            } catch (NumberFormatException ignored) {
                throw new CommandException(instance.getMessage().getMessage("FAIL.not-number").replaceAll(VALUE_REGEX, amountStr));
            }
            iStack.setAmount(amount);
        }
        if (item.getType() == ItemUtil.ItemType.VANILLA) {
            iStack.setItemMeta(null);
        }

        int free = ItemUtil.getFreeSpace(player, iStack);
        if (free == -1) {
            throw new CommandException(instance.getMessage().getMessage("FAIL.inventory-is-full"));
        }
        iStack.setAmount(free);

        storage.subtract(materialKey, free);
        ItemUtil.giveItem(player, iStack);

        player.sendMessage(instance.getMessage().getMessage("SUCCESS.withdrew-item")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(free))
                .replaceAll(ITEM_REGEX, instance.getSetting().getNameFormatted(materialKey, true)));
    }

}
