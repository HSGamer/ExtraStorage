package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.config.SettingConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;

public final class VaultHook extends AbstractEconomyHook {

    private final VaultSession session;

    public VaultHook(ExtraStorage plugin) {
        super(plugin);
        VaultSession vaultSession = null;
        VaultSession vault2Session = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            net.milkbowl.vault.economy.Economy econ = (rsp != null) ? rsp.getProvider() : null;
            if (econ != null) {
                vaultSession = (player, price) -> econ.depositPlayer(player, price).transactionSuccess();
            }

            try {
                Class.forName("net.milkbowl.vault2.economy.Economy");
                RegisteredServiceProvider<net.milkbowl.vault2.economy.Economy> rsp2 = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault2.economy.Economy.class);
                net.milkbowl.vault2.economy.Economy econ2 = (rsp2 != null) ? rsp2.getProvider() : null;
                if (econ2 != null) {
                    if (econ2.hasMultiCurrencySupport()) {
                        String currency = plugin.get(SettingConfig.class).economy().currency();
                        if (econ2.hasCurrency(currency)) {
                            vault2Session = (player, price) -> econ2.deposit(instance.getName(), player.getUniqueId(), player.getWorld().getName(), BigDecimal.valueOf(price)).transactionSuccess();
                        } else {
                            plugin.getLogger().warning("The currency '" + currency + "' is not supported! VaultUnlocked hook ignored!");
                        }
                    } else {
                        vault2Session = (player, price) -> econ2.deposit(instance.getName(), player.getUniqueId(), BigDecimal.valueOf(price)).transactionSuccess();
                    }
                }
            } catch (Exception e) {
                vault2Session = null;
            }
        }
        this.session = vault2Session != null ? vault2Session : vaultSession;
    }

    @Override
    public boolean isHooked() {
        return session != null;
    }

    @Override
    public int getAmount(ItemStack item) {
        if (!isHooked()) return 0;
        Worth worth = lookupWorth(item);
        return worth != null ? worth.getQuantity() : 0;
    }

    @Override
    protected double getRawPrice(Player player, ItemStack item, int amount) {
        Worth worth = lookupWorth(item);
        if (worth == null) return -1;
        return worth.getPrice() / worth.getQuantity() * amount;
    }

    @Override
    protected boolean deposit(Player player, double price) {
        if (session == null) {
            throw new IllegalStateException("No Vault economy session available");
        }
        return session.deposit(player, price);
    }

    private interface VaultSession {
        boolean deposit(Player player, double price);
    }
}
