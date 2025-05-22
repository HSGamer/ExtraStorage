package me.hsgamer.extrastorage.commands.subs.player;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.gui.PartnerGui;
import me.hsgamer.extrastorage.gui.StorageGui;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.util.Collection;

@Command(value = "partner", permission = Constants.PLAYER_PARTNER_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class PartnerCmd
        extends CommandListener<ExtraStorage> {

    private final UserManager manager;

    public PartnerCmd() {
        this.manager = instance.getUserManager();
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();
        User user = manager.getUser(player);

        if (context.getArgsLength() == 0) {
            new PartnerGui(player, 1).open();
            return;
        }

        String args0 = context.getArgs(0).toLowerCase();
        OfflinePlayer target;
        User partner;
        switch (args0) {
            case "add":
                if (context.getArgsLength() == 1) {
                    context.sendMessage(Message.getMessage("FAIL.must-enter-player"));
                    return;
                }

                target = Bukkit.getServer().getOfflinePlayer(context.getArgs(1));
                partner = manager.getUser(target);
                if (partner == null) {
                    context.sendMessage(Message.getMessage("FAIL.player-not-found"));
                    return;
                }
                if (target.getName().equals(player.getName())) {
                    context.sendMessage(Message.getMessage("FAIL.not-yourself"));
                    return;
                }
                if (user.isPartner(target.getUniqueId())) {
                    context.sendMessage(Message.getMessage("FAIL.already-partner"));
                    return;
                }

                user.addPartner(target.getUniqueId());
                context.sendMessage(Message.getMessage("SUCCESS.made-partner").replaceAll(Utils.getRegex("player"), target.getName()));
                if (target.isOnline()) target.getPlayer().sendMessage(Message.getMessage("SUCCESS.being-partner")
                        .replaceAll(Utils.getRegex("player"), player.getName())
                        .replaceAll(Utils.getRegex("label"), context.getLabel()));
                break;
            case "remove":
                if (context.getArgsLength() == 1) {
                    context.sendMessage(Message.getMessage("FAIL.must-enter-player"));
                    return;
                }

                target = Bukkit.getServer().getOfflinePlayer(context.getArgs(1));
                partner = manager.getUser(target);
                if (partner == null) {
                    context.sendMessage(Message.getMessage("FAIL.player-not-found"));
                    return;
                }
                if (target.getName().equals(player.getName())) {
                    context.sendMessage(Message.getMessage("FAIL.not-yourself"));
                    return;
                }
                if (!user.isPartner(target.getUniqueId())) {
                    context.sendMessage(Message.getMessage("FAIL.not-partner"));
                    return;
                }

                user.removePartner(target.getUniqueId());
                context.sendMessage(Message.getMessage("SUCCESS.removed-partner").replaceAll(Utils.getRegex("player"), target.getName()));
                if (target.isOnline()) {
                    Player p = target.getPlayer();
                    p.sendMessage(Message.getMessage("SUCCESS.no-longer-partner").replaceAll(Utils.getRegex("player"), player.getName()));
                    InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                    if (holder instanceof StorageGui) {
                        StorageGui gui = (StorageGui) holder;
                        if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                    }
                }

                break;
            case "clear":
                Collection<Partner> partners = user.getPartners();
                if (partners.size() < 1) {
                    context.sendMessage(Message.getMessage("FAIL.partners-list-empty"));
                    return;
                }
                for (Partner pn : partners) {
                    OfflinePlayer offPlayer = pn.getOfflinePlayer();
                    if (!offPlayer.isOnline()) continue;

                    Player p = offPlayer.getPlayer();
                    p.sendMessage(Message.getMessage("SUCCESS.no-longer-partner").replaceAll(Utils.getRegex("player"), player.getName()));
                    InventoryHolder holder = p.getOpenInventory().getTopInventory().getHolder();
                    if (holder instanceof StorageGui) {
                        StorageGui gui = (StorageGui) holder;
                        if (gui.getPartner().getUUID().equals(player.getUniqueId())) p.closeInventory();
                    }
                }
                user.clearPartners();
                context.sendMessage(Message.getMessage("SUCCESS.cleanup-partners-list"));
                break;
        }
    }

}
