package me.hsgamer.extrastorage.hooks.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;

public final class VaultHook extends WorthEconomyHook {

    private boolean setup = false;
    private VaultSession vaultSession;
    private VaultSession vault2Session;

    public VaultHook() {
        if (this.isHooked()) {
            instance.getLogger().info("Using Vault as economy provider.");
        } else {
            instance.getLogger().severe("Could not find dependency: Vault. Please install it then try again!");
        }
    }

    @Override
    public boolean isHooked() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        if (!setup) {
            {
                RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                net.milkbowl.vault.economy.Economy econ = (rsp != null) ? rsp.getProvider() : null;
                if (econ != null) {
                    vaultSession = (player, price) -> econ.depositPlayer(player, price).transactionSuccess();
                }
            }

            {
                try {
                    Class.forName("net.milkbowl.vault2.economy.Economy");
                    RegisteredServiceProvider<net.milkbowl.vault2.economy.Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault2.economy.Economy.class);
                    net.milkbowl.vault2.economy.Economy econ = (rsp != null) ? rsp.getProvider() : null;
                    if (econ != null) {
                        if (econ.hasMultiCurrencySupport()) {
                            String currency = instance.getSetting().getCurrency();
                            if (econ.hasCurrency(currency)) {
                                vault2Session = (player, price) -> econ.deposit(instance.getName(), player.getUniqueId(), player.getWorld().getName(), BigDecimal.valueOf(price)).transactionSuccess();
                            } else {
                                instance.getLogger().warning("The currency '" + currency + "' is not supported! VaultUnlocked hook ignored!");
                            }
                        } else {
                            vault2Session = (player, price) -> econ.deposit(instance.getName(), player.getUniqueId(), BigDecimal.valueOf(price)).transactionSuccess();
                        }
                    }
                } catch (Exception e) {
                    vault2Session = null;
                }
            }

            setup = true;
        }
        return vaultSession != null || vault2Session != null;
    }

    @Override
    protected boolean deposit(Player player, double price) {
        VaultSession vaultSession = this.vault2Session != null ? vault2Session : this.vaultSession;
        assert vaultSession != null;
        return vaultSession.deposit(player, price);
    }

    private interface VaultSession {
        boolean deposit(Player player, double price);
    }
}
