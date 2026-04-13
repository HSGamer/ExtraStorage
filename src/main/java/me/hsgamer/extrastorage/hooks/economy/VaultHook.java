package me.hsgamer.extrastorage.hooks.economy;

import net.milkbowl.vault.economy.Economy;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook extends WorthEconomyHook {

    private boolean setup = false;
    private Economy econ;

    public VaultHook() {
        if (this.isHooked()) {
            instance.getLogger().info("Using Vault as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "Vault"));
        } else instance.getLogger().severe("Could not find dependency: Vault. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        if (!setup) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            econ = (rsp != null) ? rsp.getProvider() : null;
            setup = true;
        }
        return econ != null;
    }

    @Override
    protected boolean deposit(Player player, double price) {
        return econ.depositPlayer(player, price).transactionSuccess();
    }
}
