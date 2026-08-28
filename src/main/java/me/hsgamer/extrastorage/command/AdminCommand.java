package me.hsgamer.extrastorage.command;

import io.github.projectunified.craftcommand.annotation.Command;
import io.github.projectunified.craftcommand.annotation.Default;
import io.github.projectunified.craftcommand.annotation.Suggest;
import io.github.projectunified.craftcommand.bukkit.annotation.Permission;
import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.component.Reloadable;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.gui.WhitelistGUI;
import me.hsgamer.extrastorage.manager.CommandManager;
import me.hsgamer.extrastorage.manager.UserManager;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Command(value = "esadmin", description = "Commands for administrators")
public class AdminCommand {
    private static final String VERSION_REGEX = Utils.getRegex("ver(sion)?");
    private static final String LABEL_REGEX = Utils.getRegex("label");
    private static final String PLAYER_REGEX = Utils.getRegex("player");
    private static final String ITEM_REGEX = Utils.getRegex("item");
    private static final String QUANTITY_REGEX = Utils.getRegex("quantity");
    private static final String SPACE_REGEX = Utils.getRegex("space");
    private static final Pattern ALL_PATTERN = Pattern.compile("(?ium)(\\*|-all)");
    private final ExtraStorage instance;
    private final UserManager manager;
    private final SettingConfig setting;

    public AdminCommand(ExtraStorage instance) {
        this.instance = instance;
        this.manager = instance.get(UserManager.class);
        this.setting = instance.get(SettingConfig.class);
    }

    @Default
    @Permission(Constants.ADMIN_HELP_PERMISSION)
    public void execute(CommandSender sender) {
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().header()).replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().help()).replaceAll(LABEL_REGEX, "esadmin"));
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_OPEN_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().open()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_SPACE_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().space()).replaceAll(LABEL_REGEX, "esadmin"));
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().addspace()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_ADD_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().add()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_SUBTRACT_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().subtract()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_SET_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().set()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_RESET_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().reset()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_WHITELIST_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().whitelist()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        if (sender.isOp() || sender.hasPermission(Constants.ADMIN_RELOAD_PERMISSION)) {
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().admin().reload()).replaceAll(LABEL_REGEX, "esadmin"));
        }
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).help().footer()).replaceAll(VERSION_REGEX, instance.getDescription().getVersion()));
    }

    @Command("open")
    @Permission(Constants.ADMIN_OPEN_PERMISSION)
    public void open(Player sender, String targetName) {
        User user = resolveTargetUser(targetName);
        instance.get(StorageGUI.class).openFor(sender, user);
    }

    @Command("space")
    @Permission(Constants.ADMIN_SPACE_PERMISSION)
    public void space(CommandSender sender, long amount, @Default String target) {
        if (setting.maxSpace() == -1) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().maxSpaceNotUsed()));
        }

        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            user.getStorage().setSpace(amount);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().spaceChanged()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
            return;
        }

        if (ALL_PATTERN.matcher(target).matches()) {
            AsyncScheduler.get(instance).run(() -> {
                for (User user : manager.getUsers()) {
                    user.getStorage().setSpace(amount);
                    notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().spaceChanged()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
                }
            });
            return;
        }

        User user = resolveTargetUser(target);
        user.getStorage().setSpace(amount);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().spaceChanged()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().spaceChanged()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
    }

    @Command("addspace")
    @Permission(Constants.ADMIN_SPACE_PERMISSION)
    public void addSpace(CommandSender sender, long amount, @Default String target) {
        if (setting.maxSpace() == -1) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().maxSpaceNotUsed()));
        }

        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            Storage storage = user.getStorage();
            if (checkIntLimit(storage.getSpace(), amount)) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().spaceExceeded()));
            }
            storage.addSpace(amount);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().spaceIncreased()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
            return;
        }

        if (ALL_PATTERN.matcher(target).matches()) {
            AsyncScheduler.get(instance).run(() -> {
                for (User user : manager.getUsers()) {
                    Storage storage = user.getStorage();
                    if (checkIntLimit(storage.getSpace(), amount)) continue;
                    storage.addSpace(amount);
                    notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().spaceIncreased()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
                }
            });
            return;
        }

        User user = resolveTargetUser(target);
        Storage storage = user.getStorage();
        if (checkIntLimit(storage.getSpace(), amount)) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().spaceExceeded()));
        }
        storage.addSpace(amount);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().spaceIncreased()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().spaceIncreased()).replaceAll(SPACE_REGEX, Digital.formatThousands(amount)));
    }

    Collection<String> suggestMaterials(Player sender) {
        User user = instance.get(UserManager.class).getUser(sender);
        return user.getStorage().getItems().keySet();
    }

    @Command("add")
    @Permission(Constants.ADMIN_ADD_PERMISSION)
    public void add(CommandSender sender, @Suggest("suggestMaterials") String materialKey, long amount, @Default String target) {
        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            Storage storage = user.getStorage();

            requireItem(storage, materialKey, user.getOfflinePlayer());
            long freeSpace = clampFreeSpace(storage, amount);
            storage.add(materialKey, freeSpace);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().add().self())
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        User user = resolveTargetUser(target);
        OfflinePlayer player = user.getOfflinePlayer();
        Storage storage = user.getStorage();

        requireItem(storage, materialKey, player);
        long freeSpace = clampFreeSpace(storage, amount);
        storage.add(materialKey, freeSpace);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().add().sender())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().add().target())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("addrnd")
    @Permission(Constants.ADMIN_ADD_PERMISSION)
    public void addRnd(CommandSender sender, @Suggest("suggestMaterials") String materialKey, @Default String target) {
        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            Storage storage = user.getStorage();

            if (materialKey.equals("*")) {
                String[] keys = storage.getItems().keySet().toArray(new String[0]);
                long total = addQuantity(storage, keys);
                if (total < 1) {
                    throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().storageIsFull()));
                }
                sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().add().self())
                        .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                        .replaceAll(ITEM_REGEX, "all"));
                return;
            }

            requireItem(storage, materialKey, user.getOfflinePlayer());
            long amount = Digital.random(1000, 100000);
            long freeSpace = clampFreeSpace(storage, amount);
            storage.add(materialKey, freeSpace);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().add().self())
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        User user = resolveTargetUser(target);
        OfflinePlayer player = user.getOfflinePlayer();
        Storage storage = user.getStorage();

        if (materialKey.equals("*")) {
            String[] keys = storage.getItems().keySet().toArray(new String[0]);
            long total = addQuantity(storage, keys);
            if (total < 1) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().storageIsFull()));
            }
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().add().sender())
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                    .replaceAll(ITEM_REGEX, "all")
                    .replaceAll(PLAYER_REGEX, player.getName()));
            notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().add().target())
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(total))
                    .replaceAll(ITEM_REGEX, "all")
                    .replaceAll(PLAYER_REGEX, sender.getName()));
            return;
        }

        requireItem(storage, materialKey, player);
        long amount = Digital.random(1000, 100000);
        long freeSpace = clampFreeSpace(storage, amount);
        storage.add(materialKey, freeSpace);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().add().sender())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().add().target())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(freeSpace))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("subtract")
    @Permission(Constants.ADMIN_SUBTRACT_PERMISSION)
    public void subtract(CommandSender sender, @Suggest("suggestMaterials") String materialKey, long amount, @Default String target) {
        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            Storage storage = user.getStorage();

            Item item = requireItem(storage, materialKey, user.getOfflinePlayer());
            int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
            if (current < 1) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notEnoughItem()).replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            }
            int subtractAmount = (int) Math.min(amount, current);
            storage.subtract(materialKey, subtractAmount);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().subtract().self())
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(subtractAmount))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        User user = resolveTargetUser(target);
        OfflinePlayer player = user.getOfflinePlayer();
        Storage storage = user.getStorage();

        Item item = requireItem(storage, materialKey, player);
        int current = (int) Math.min(item.getQuantity(), Integer.MAX_VALUE);
        if (current < 1) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().notEnoughItem()).replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
        }
        int subtractAmount = (int) Math.min(amount, current);
        storage.subtract(materialKey, subtractAmount);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().subtract().sender())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(subtractAmount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().subtract().target())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(subtractAmount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("set")
    @Permission(Constants.ADMIN_SET_PERMISSION)
    public void set(CommandSender sender, @Suggest("suggestMaterials") String materialKey, long amount, @Default String target) {
        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            Storage storage = user.getStorage();

            Item item = requireItem(storage, materialKey, user.getOfflinePlayer());
            long space = storage.getSpace();
            if (space != -1) {
                long usedSpace = storage.getUsedSpace() - item.getQuantity();
                if ((storage.getUsedSpace() == usedSpace) && storage.isMaxSpace()) {
                    throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().storageIsFull()));
                }
                if ((usedSpace + amount) > space) amount = (space - usedSpace);
            }
            storage.set(materialKey, amount);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().set().self())
                    .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                    .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        User user = resolveTargetUser(target);
        OfflinePlayer player = user.getOfflinePlayer();
        Storage storage = user.getStorage();

        Item item = requireItem(storage, materialKey, player);
        long space = storage.getSpace();
        if (space != -1) {
            long usedSpace = storage.getUsedSpace() - item.getQuantity();
            if ((storage.getUsedSpace() == usedSpace) && storage.isMaxSpace()) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().storageIsFull()));
            }
            if ((usedSpace + amount) > space) amount = (space - usedSpace);
        }
        storage.set(materialKey, amount);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().set().sender())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().set().target())
                .replaceAll(QUANTITY_REGEX, Digital.formatThousands(amount))
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    Collection<String> suggestMaterialsForReset(Player player) {
        List<String> suggestions = new ArrayList<>(suggestMaterials(player));
        suggestions.add("*");
        return suggestions;
    }

    @Command("reset")
    @Permission(Constants.ADMIN_RESET_PERMISSION)
    public void reset(CommandSender sender, @Suggest("suggestMaterialsForReset") String materialKey, @Default String target) {
        boolean isAll = ALL_PATTERN.matcher(materialKey).matches();

        if (target.isEmpty()) {
            User user = resolvePlayerUser(sender);
            Storage storage = user.getStorage();

            if (isAll) {
                storage.reset(null);
                sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().reset().all()));
                return;
            }

            requireItem(storage, materialKey, user.getOfflinePlayer());
            storage.reset(materialKey);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().reset().self()).replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true)));
            return;
        }

        User user = resolveTargetUser(target);
        OfflinePlayer player = user.getOfflinePlayer();
        Storage storage = user.getStorage();

        if (isAll) {
            storage.reset(null);
            sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().reset().allSender()).replaceAll(PLAYER_REGEX, player.getName()));
            notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().reset().all()));
            return;
        }

        requireItem(storage, materialKey, player);
        storage.reset(materialKey);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().reset().sender())
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, player.getName()));
        notifyOnline(user, Utils.formatMessage(instance.get(MessageConfig.class).success().reset().target())
                .replaceAll(ITEM_REGEX, setting.getNameFormatted(materialKey, true))
                .replaceAll(PLAYER_REGEX, sender.getName()));
    }

    @Command("whitelist")
    @Permission(Constants.ADMIN_WHITELIST_PERMISSION)
    public void whitelist(Player sender) {
        instance.get(WhitelistGUI.class).openFor(sender);
    }

    @Command(value = "reload", aliases = {"rld", "rl"})
    @Permission(Constants.ADMIN_RELOAD_PERMISSION)
    public void reload(CommandSender sender) {
        instance.call(Reloadable.class, Reloadable::reload);
        sender.sendMessage(Utils.formatMessage(instance.get(MessageConfig.class).success().configReload()));
    }

    private User resolveTargetUser(String target) {
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(target);
        if (!player.hasPlayedBefore()) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().playerNotFound()));
        }
        return manager.getUser(player);
    }

    private User resolvePlayerUser(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new io.github.projectunified.craftcommand.exception.CommandException(instance.get(CommandManager.class).getCommandManager().formatMessage("invalid-sender", "Only %s can execute this command.", "Player"));
        }
        return manager.getUser((Player) sender);
    }

    private Item requireItem(Storage storage, String materialKey, OfflinePlayer player) {
        Optional<Item> optional = storage.getItem(materialKey);
        if (!optional.isPresent()) {
            throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().itemNotInStorage()).replaceAll(PLAYER_REGEX, player.getName()));
        }
        return optional.get();
    }

    private long clampFreeSpace(Storage storage, long amount) {
        long freeSpace = storage.getFreeSpace();
        if (freeSpace != -1) {
            if (freeSpace < 1) {
                throw new CommandException(Utils.formatMessage(instance.get(MessageConfig.class).fail().storageIsFull()));
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
        try {
            Math.addExact(value, increValue);
            return false;
        } catch (ArithmeticException e) {
            return true;
        }
    }

}
