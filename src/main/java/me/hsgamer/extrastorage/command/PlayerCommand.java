package me.hsgamer.extrastorage.command;

import io.github.projectunified.craftcommand.annotation.Command;
import io.github.projectunified.craftcommand.annotation.Default;
import io.github.projectunified.craftcommand.annotation.Resolve;
import io.github.projectunified.craftcommand.annotation.Suggest;
import io.github.projectunified.craftcommand.bukkit.annotation.Permission;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.FilterGUI;
import me.hsgamer.extrastorage.gui.PartnerGUI;
import me.hsgamer.extrastorage.gui.SellGUI;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.manager.HookManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Command(value = "extrastorage", aliases = {"exstorage", "storage", "es", "kho"}, description = "Commands for players")
public class PlayerCommand {
    private static final String VERSION_REGEX = Utils.getRegex("ver(sion)?");
    private static final String LABEL_REGEX = Utils.getRegex("label");
    private static final String PLAYER_REGEX = Utils.getRegex("player");
    private static final String ITEM_REGEX = Utils.getRegex("item");
    private static final String AMOUNT_REGEX = Utils.getRegex("amount");
    private static final String QUANTITY_REGEX = Utils.getRegex("quantity");
    private static final String PRICE_REGEX = Utils.getRegex("price");
    private static final String VALUE_REGEX = Utils.getRegex("value");
    private static final String STATUS_REGEX = Utils.getRegex("status");
    private final ExtraStorage instance;

    public PlayerCommand(ExtraStorage instance) {
        this.instance = instance;
    }


    @Default
    @Permission(Constants.PLAYER_OPEN_PERMISSION)
    public void execute(User user, @Default String target) {
        Player player = user.getPlayer();

        if (target.isEmpty()) {
            instance.get(StorageGUI.class).openFor(player, null);
            return;
        }

        OfflinePlayer targetPlayer = Bukkit.getServer().getOfflinePlayer(target);
        if (!targetPlayer.hasPlayedBefore()) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().playerNotFound()));
        }
        User targetUser = instance.get(UserManager.class).getUser(targetPlayer);

        if (target.equals(player.getName())) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notYourself()));
        }

        if (!targetUser.isPartner(player.getUniqueId())) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().playerNotPartner()).replaceAll(PLAYER_REGEX, target));
        }

        instance.get(StorageGUI.class).openFor(player, targetUser);
    }

    @Command(value = "help", aliases = {"?"})
    @Permission(Constants.PLAYER_HELP_PERMISSION)
    public void help(CommandSender sender) {
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().header()).replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().help()).replaceAll(LABEL_REGEX, "extrastorage"));
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_OPEN_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().open()).replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_TOGGLE_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().toggle()).replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_FILTER_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().filter()).replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_PARTNER_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().partner()).replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_SELL_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().sell()).replaceAll(LABEL_REGEX, "extrastorage"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.PLAYER_WITHDRAW_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().player().withdraw()).replaceAll(LABEL_REGEX, "extrastorage"));
        }
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().footer()).replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
    }

    @Command("toggle")
    @Permission(Constants.PLAYER_TOGGLE_PERMISSION)
    public void toggle(User user) {
        Storage storage = user.getStorage();
        boolean toggled = !storage.getStatus();
        storage.setStatus(toggled);

        user.getPlayer().sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().storageUsageToggled())
                .replaceAll(STATUS_REGEX, Utils.formatMessage(toggled ? instance.get(MessageConfig.class).status().enabled() : instance.get(MessageConfig.class).status().disabled())));
    }

    @Command("filter")
    @Permission(Constants.PLAYER_FILTER_PERMISSION)
    public void filter(Player sender) {
        instance.get(FilterGUI.class).openFor(sender);
    }

    List<String> suggestUserMaterials(@Resolve User user) {
        return user.getStorage()
                .getItems()
                .values()
                .stream()
                .filter(item -> item.getQuantity() > 0)
                .map(Item::getKey)
                .collect(Collectors.toList());
    }

    @Command("sell")
    @Permission(Constants.PLAYER_SELL_PERMISSION)
    public void sell(User user, @Default @Suggest("suggestUserMaterials") String materialKey, @Default String amountStr) {
        Player player = user.getPlayer();

        if (materialKey.isEmpty()) {
            instance.get(SellGUI.class).openFor(player);
            return;
        }

        Storage storage = user.getStorage();
        Optional<Item> optional = storage.getItem(materialKey);
        if (!optional.isPresent()) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().itemNotInStorage()).replaceAll(PLAYER_REGEX, player.getName()));
        }

        Item item = optional.get();
        int quantity = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
        if (quantity < 1) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notEnoughItem()).replaceAll(ITEM_REGEX, instance.get(SettingConfig.class).getNameFormatted(materialKey, true)));
        }

        if (amountStr.isEmpty()) {
            instance.get(HookManager.class).getEconomyProvider()
                    .sellItem(player, item.getItem(), quantity, result -> {
                        if (!result.isSuccess()) {
                            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().cannotBeSold()));
                        }
                        storage.subtract(materialKey, quantity);
                        player.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().itemSold())
                                .replaceAll(AMOUNT_REGEX, Digital.formatThousands(quantity))
                                .replaceAll(ITEM_REGEX, instance.get(SettingConfig.class).getNameFormatted(materialKey, true))
                                .replaceAll(PRICE_REGEX, Digital.formatDouble("###,###.##", result.getPrice())));
                    });
            return;
        }

        int amount;
        try {
            amount = Digital.getBetween(1, quantity, Integer.parseInt(amountStr));
        } catch (NumberFormatException ignored) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notNumber()).replaceAll(VALUE_REGEX, amountStr));
        }

        instance.get(HookManager.class).getEconomyProvider()
                .sellItem(player, item.getItem(), amount, result -> {
                    if (!result.isSuccess()) {
                        throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().cannotBeSold()));
                    }
                    storage.subtract(materialKey, amount);
                    player.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().itemSold())
                            .replaceAll(AMOUNT_REGEX, Digital.formatThousands(amount))
                            .replaceAll(ITEM_REGEX, instance.get(SettingConfig.class).getNameFormatted(materialKey, true))
                            .replaceAll(PRICE_REGEX, Digital.formatDouble("###,###.##", result.getPrice())));
                });
    }

    @Command("withdraw")
    @Permission(Constants.PLAYER_WITHDRAW_PERMISSION)
    public void withdraw(User user, @Suggest("suggestUserMaterials") String materialKey, @Default String amountStr) {
        Player player = user.getPlayer();
        Storage storage = user.getStorage();

        Optional<Item> optional = storage.getItem(materialKey);
        if (!optional.isPresent()) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().itemNotInStorage()).replaceAll(PLAYER_REGEX, player.getName()));
        }
        Item item = optional.get();
        if (!item.isLoaded()) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().itemNotInStorage()).replaceAll(PLAYER_REGEX, player.getName()));
        }

        int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
        if (current < 1) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notEnoughItem()).replaceAll(ITEM_REGEX, instance.get(SettingConfig.class).getNameFormatted(materialKey, true)));
        }
        ItemStack iStack = item.getItem().clone();

        if (amountStr.isEmpty()) {
            iStack.setAmount(current);
        } else {
            int amount;
            try {
                amount = Digital.getBetween(1, current, Integer.parseInt(amountStr));
            } catch (NumberFormatException ignored) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notNumber()).replaceAll(VALUE_REGEX, amountStr));
            }
            iStack.setAmount(amount);
        }
        if (item.getType() == ItemUtil.ItemType.VANILLA) {
            iStack.setItemMeta(null);
        }

        int free = ItemUtil.getFreeSpace(player, iStack);
        if (free == -1) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().inventoryIsFull()));
        }
        iStack.setAmount(free);

        storage.subtract(materialKey, free);
        ItemUtil.giveItem(player, iStack);

        player.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().withdrewItem())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(free))
                .replaceAll(ITEM_REGEX, instance.get(SettingConfig.class).getNameFormatted(materialKey, true)));
    }

    @Command("partner")
    @Permission(Constants.PLAYER_PARTNER_PERMISSION)
    public class PartnerCommand {
        @Default
        public void execute(User user) {
            instance.get(PartnerGUI.class).openFor(user.getPlayer());
        }

        @Command("add")
        public void add(User user, String targetName) {
            Player player = user.getPlayer();

            OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().playerNotFound()));
            }
            if (targetName.equals(player.getName())) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notYourself()));
            }
            if (user.isPartner(target.getUniqueId())) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().alreadyPartner()));
            }

            user.addPartner(target.getUniqueId());
            player.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().madePartner()).replaceAll(PLAYER_REGEX, target.getName()));
            if (target.isOnline()) {
                target.getPlayer().sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().beingPartner())
                        .replaceAll(PLAYER_REGEX, player.getName())
                        .replaceAll(LABEL_REGEX, "extrastorage"));
            }
        }

        @Command("remove")
        public void remove(User user, String targetName) {
            Player player = user.getPlayer();

            OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().playerNotFound()));
            }
            if (targetName.equals(player.getName())) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notYourself()));
            }
            if (!user.isPartner(target.getUniqueId())) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notPartner()));
            }

            user.removePartner(target.getUniqueId());
            player.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().removedPartner()).replaceAll(PLAYER_REGEX, target.getName()));
            if (target.isOnline()) {
                Player p = target.getPlayer();
                p.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().noLongerPartner()).replaceAll(PLAYER_REGEX, player.getName()));
                StorageGUI.StorageData sd = instance.get(StorageGUI.class).getSessionData(p.getUniqueId());
                if (sd != null && sd.partner.getUUID().equals(player.getUniqueId())) p.closeInventory();
            }
        }

        @Command("clear")
        public void clear(User user) {
            Player player = user.getPlayer();
            Collection<Partner> partners = user.getPartners();
            if (partners.isEmpty()) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().partnersListEmpty()));
            }
            for (Partner pn : partners) {
                OfflinePlayer offPlayer = pn.getOfflinePlayer();
                if (!offPlayer.isOnline()) continue;

                Player p = offPlayer.getPlayer();
                p.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().noLongerPartner()).replaceAll(PLAYER_REGEX, player.getName()));
                StorageGUI.StorageData sd = instance.get(StorageGUI.class).getSessionData(p.getUniqueId());
                if (sd != null && sd.partner.getUUID().equals(player.getUniqueId())) p.closeInventory();
            }
            user.clearPartners();
            player.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().cleanupPartnersList()));
        }
    }

}
