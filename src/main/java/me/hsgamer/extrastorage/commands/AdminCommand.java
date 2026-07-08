package me.hsgamer.extrastorage.commands;

import io.github.projectunified.craftcommand.annotation.*;
import io.github.projectunified.craftcommand.bukkit.annotation.Permission;
import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.gui.WhitelistGUI;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.regex.Pattern;

@Command(value = "esadmin", description = "Commands for administrators")
public class AdminCommand {

    private final ExtraStorage instance = ExtraStorage.getInstance();
    private final UserManager manager = instance.getUserManager();
    private final Setting setting = instance.getSetting();

    private static final String VERSION_REGEX = Utils.getRegex("ver(sion)?");
    private static final String LABEL_REGEX = Utils.getRegex("label");
    private static final String PLAYER_REGEX = Utils.getRegex("player");
    private static final String ITEM_REGEX = Utils.getRegex("item");
    private static final String QUANTITY_REGEX = Utils.getRegex("quantity");
    private static final String SPACE_REGEX = Utils.getRegex("space");
    private static final Pattern ALL_PATTERN = Pattern.compile("(?ium)(\\*|-all)");

    @Default
    @Permission(Constants.ADMIN_HELP_PERMISSION)
    public void execute(CommandSender sender) {
        sender.sendMessage(Message.getMessage("HELP.header").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
        sender.sendMessage(Message.getMessage("HELP.Admin.help").replaceAll(LABEL_REGEX, "esadmin"));
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_OPEN_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.open").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_SPACE_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.space").replaceAll(LABEL_REGEX, "esadmin"));
            sender.sendMessage(Message.getMessage("HELP.Admin.addspace").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_ADD_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.add").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_SUBTRACT_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.subtract").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_SET_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.set").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_RESET_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.reset").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_WHITELIST_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.whitelist").replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_RELOAD_PERMISSION)) {
            sender.sendMessage(Message.getMessage("HELP.Admin.reload").replaceAll(LABEL_REGEX, "esadmin"));
        }
        sender.sendMessage(Message.getMessage("HELP.footer").replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
    }

    @Command("open")
    @Permission(Constants.ADMIN_OPEN_PERMISSION)
    public void open(Player sender, String targetName) {
        User user = resolveTargetUser(targetName);
        new StorageGUI(sender, user).open();
    }

    @Command("space")
    @Permission(Constants.ADMIN_SPACE_PERMISSION)
    public void space(Player sender, long amount, @Default String target) {
        if (setting.getMaxSpace() == -1) {
            throw new CommandException(Message.getMessage("FAIL.max-space-not-used"));
        }

        if (target == null) {
            User user = manager.getUser(sender);
            user.getStorage().setSpace(amount);
            sender.sendMessage(Message.getMessage("SUCCESS.space-changed").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
            return;
        }

        if (ALL_PATTERN.matcher(target).matches()) {
            AsyncScheduler.get(instance).run(() -> {
                for (User user : manager.getUsers()) {
                    user.getStorage().setSpace(amount);
                    notifyOnline(user, Message.getMessage("SUCCESS.space-changed").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
                }
            });
            return;
        }

        User user = resolveTargetUser(target);
        user.getStorage().setSpace(amount);
        sender.sendMessage(Message.getMessage("SUCCESS.space-changed").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
        notifyOnline(user, Message.getMessage("SUCCESS.space-changed").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
    }

    @Command("addspace")
    @Permission(Constants.ADMIN_SPACE_PERMISSION)
    public void addSpace(Player sender, long amount, @Default String target) {
        if (setting.getMaxSpace() == -1) {
            throw new CommandException(Message.getMessage("FAIL.max-space-not-used"));
        }

        if (target == null) {
            User user = manager.getUser(sender);
            Storage storage = user.getStorage();
            if (checkIntLimit(storage.getSpace(), amount)) {
                throw new CommandException(Message.getMessage("FAIL.space-exceeded"));
            }
            storage.addSpace(amount);
            sender.sendMessage(Message.getMessage("SUCCESS.space-increased").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
            return;
        }

        if (ALL_PATTERN.matcher(target).matches()) {
            AsyncScheduler.get(instance).run(() -> {
                for (User user : manager.getUsers()) {
                    Storage storage = user.getStorage();
                    if (checkIntLimit(storage.getSpace(), amount)) continue;
                    storage.addSpace(amount);
                    notifyOnline(user, Message.getMessage("SUCCESS.space-increased").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
                }
            });
            return;
        }

        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();
        if (checkIntLimit(storage.getSpace(), amount)) {
            throw new CommandException(Message.getMessage("FAIL.space-exceeded"));
        }
        storage.addSpace(amount);
        sender.sendMessage(Message.getMessage("SUCCESS.space-increased").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
        notifyOnline(user, Message.getMessage("SUCCESS.space-increased").replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
    }

    @Command("add")
    @Permission(Constants.ADMIN_ADD_PERMISSION)
    public void add(Player sender, String materialKey, long amount, @Default String target) {
        if (target == null) {
            User user = manager.getUser(sender);
            Storage storage = user.getStorage();

            Item item = requireItem(storage, materialKey, sender);
            long freeSpace = clampFreeSpace(storage, amount);
            storage.add(materialKey, freeSpace);
            sender.sendMessage(Message.getMessage("SUCCESS.Add.self")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();

        Item item = requireItem(storage, materialKey, player);
        long freeSpace = clampFreeSpace(storage, amount);
        storage.add(materialKey, freeSpace);
        sender.sendMessage(Message.getMessage("SUCCESS.Add.sender")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Message.getMessage("SUCCESS.Add.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("addrnd")
    @Permission(Constants.ADMIN_ADD_PERMISSION)
    public void addRnd(Player sender, String materialKey, @Default String target) {
        if (target == null) {
            User user = manager.getUser(sender);
            Storage storage = user.getStorage();

            if (materialKey.equals("*")) {
                String[] keys = storage.getItems().keySet().toArray(new String[0]);
                long total = addQuantity(storage, keys);
                if (total < 1) {
                    throw new CommandException(Message.getMessage("FAIL.storage-is-full"));
                }
                sender.sendMessage(Message.getMessage("SUCCESS.Add.self")
                        .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                        .replaceAll(ITEM_REGEX, "all"));
                return;
            }

            requireItem(storage, materialKey, sender);
            long amount = Digital.random(1000, 100000);
            long freeSpace = clampFreeSpace(storage, amount);
            storage.add(materialKey, freeSpace);
            sender.sendMessage(Message.getMessage("SUCCESS.Add.self")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();

        if (materialKey.equals("*")) {
            String[] keys = storage.getItems().keySet().toArray(new String[0]);
            long total = addQuantity(storage, keys);
            if (total < 1) {
                throw new CommandException(Message.getMessage("FAIL.storage-is-full"));
            }
            sender.sendMessage(Message.getMessage("SUCCESS.Add.sender")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                    .replaceAll(ITEM_REGEX, "all")
                    .replaceAll(PLAYER_REGEX, player.getName()));
            notifyOnline(user, Message.getMessage("SUCCESS.Add.target")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                    .replaceAll(ITEM_REGEX, "all")
                    .replaceAll(PLAYER_REGEX, sender.getName()));
            return;
        }

        requireItem(storage, materialKey, player);
        long amount = Digital.random(1000, 100000);
        long freeSpace = clampFreeSpace(storage, amount);
        storage.add(materialKey, freeSpace);
        sender.sendMessage(Message.getMessage("SUCCESS.Add.sender")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Message.getMessage("SUCCESS.Add.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("subtract")
    @Permission(Constants.ADMIN_SUBTRACT_PERMISSION)
    public void subtract(Player sender, String materialKey, long amount, @Default String target) {
        if (target == null) {
            User user = manager.getUser(sender);
            Storage storage = user.getStorage();

            Item item = requireItem(storage, materialKey, sender);
            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
            if (current < 1) {
                throw new CommandException(Message.getMessage("FAIL.not-enough-item").replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            }
            int subtractAmount = (int) Math.min(amount, current);
            storage.subtract(materialKey, subtractAmount);
            sender.sendMessage(Message.getMessage("SUCCESS.Subtract.self")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(subtractAmount))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();

        Item item = requireItem(storage, materialKey, player);
        int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
        if (current < 1) {
            throw new CommandException(Message.getMessage("FAIL.not-enough-item").replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
        }
        int subtractAmount = (int) Math.min(amount, current);
        storage.subtract(materialKey, subtractAmount);
        sender.sendMessage(Message.getMessage("SUCCESS.Subtract.sender")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(subtractAmount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Message.getMessage("SUCCESS.Subtract.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(subtractAmount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("set")
    @Permission(Constants.ADMIN_SET_PERMISSION)
    public void set(Player sender, String materialKey, long amount, @Default String target) {
        if (target == null) {
            User user = manager.getUser(sender);
            Storage storage = user.getStorage();

            Item item = requireItem(storage, materialKey, sender);
            long space = storage.getSpace();
            if (space != -1) {
                long usedSpace = storage.getUsedSpace() - item.getQuantity();
                if ((storage.getUsedSpace() == usedSpace) && storage.isMaxSpace()) {
                    throw new CommandException(Message.getMessage("FAIL.storage-is-full"));
                }
                if ((usedSpace + amount) > space) amount = (space - usedSpace);
            }
            storage.set(materialKey, amount);
            sender.sendMessage(Message.getMessage("SUCCESS.Set.self")
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();

        Item item = requireItem(storage, materialKey, player);
        long space = storage.getSpace();
        if (space != -1) {
            long usedSpace = storage.getUsedSpace() - item.getQuantity();
            if ((storage.getUsedSpace() == usedSpace) && storage.isMaxSpace()) {
                throw new CommandException(Message.getMessage("FAIL.storage-is-full"));
            }
            if ((usedSpace + amount) > space) amount = (space - usedSpace);
        }
        storage.set(materialKey, amount);
        sender.sendMessage(Message.getMessage("SUCCESS.Set.sender")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Message.getMessage("SUCCESS.Set.target")
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("reset")
    @Permission(Constants.ADMIN_RESET_PERMISSION)
    public void reset(Player sender, String materialKey, @Default String target) {
        boolean isAll = ALL_PATTERN.matcher(materialKey).matches();

        if (target == null) {
            User user = manager.getUser(sender);
            Storage storage = user.getStorage();

            if (isAll) {
                storage.reset(null);
                sender.sendMessage(Message.getMessage("SUCCESS.Reset.all"));
                return;
            }

            requireItem(storage, materialKey, sender);
            storage.reset(materialKey);
            sender.sendMessage(Message.getMessage("SUCCESS.Reset.self").replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();

        if (isAll) {
            storage.reset(null);
            sender.sendMessage(Message.getMessage("SUCCESS.Reset.all-sender").replaceAll(PLAYER_REGEX, player.getName()));
            notifyOnline(user, Message.getMessage("SUCCESS.Reset.all"));
            return;
        }

        requireItem(storage, materialKey, player);
        storage.reset(materialKey);
        sender.sendMessage(Message.getMessage("SUCCESS.Reset.sender")
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Message.getMessage("SUCCESS.Reset.target")
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("whitelist")
    @Permission(Constants.ADMIN_WHITELIST_PERMISSION)
    public void whitelist(Player sender) {
        new WhitelistGUI(sender).open();
    }

    @Command(value = "reload", aliases = {"rld", "rl"})
    @Permission(Constants.ADMIN_RELOAD_PERMISSION)
    public void reload(CommandSender sender) {
        instance.getSetting().reload();
        instance.getMessage().reload();
        instance.getWorthManager().reload();
        instance.loadGuiFile();
        sender.sendMessage(Message.getMessage("SUCCESS.config-reload"));
    }

    private User resolveTargetUser(String target) {
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        if (!player.hasPlayedBefore()) {
            throw new CommandException(Message.getMessage("FAIL.player-not-found"));
        }
        return manager.getUser(player);
    }

    private Item requireItem(Storage storage, String materialKey, OfflinePlayer player) {
        Optional<Item> optional = storage.getItem(materialKey);
        if (!optional.isPresent()) {
            throw new CommandException(Message.getMessage("FAIL.item-not-in-storage").replaceAll(PLAYER_REGEX, player.getName()));
        }
        return optional.get();
    }

    private long clampFreeSpace(Storage storage, long amount) {
        long freeSpace = storage.getFreeSpace();
        if (freeSpace != -1) {
            if (freeSpace < 1) {
                throw new CommandException(Message.getMessage("FAIL.storage-is-full"));
            }
            if (amount > freeSpace) amount = freeSpace;
        }
        return amount;
    }

    private void notifyOnline(User user, String message) {
        Player player = user.getPlayer();
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        } else {
            user.save();
        }
    }

    private long addQuantity(Storage storage, String... keys) {
        if ((keys == null) || (keys.length < 1)) return -1;

        long count = 0;
        for (String key : keys) {
            Optional<Item> optional = storage.getItem(key);
            if (!optional.isPresent()) continue;

            long amount = Digital.random(1000, 100000);
            long freeSpace = storage.getFreeSpace();

            if (freeSpace != -1) {
                if (freeSpace < 1) break;
                if (amount > freeSpace) amount = freeSpace;
            }

            storage.add(key, amount);
            count += amount;

            if (amount == freeSpace) break;
        }

        return ((count < 1) ? -1 : count);
    }

    private boolean checkIntLimit(long value, long increValue) {
        return (Long.MAX_VALUE - increValue) < value;
    }

}
