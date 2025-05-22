package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import net.milkbowl.vault.economy.Economy;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.function.Consumer;

public final class VaultHook
        implements EconomyProvider {

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
    public int getAmount(ItemStack item) {
        if (!this.isHooked()) return 0;
        String key = ItemUtil.toMaterialKey(item);

        Worth worth = instance.getWorthManager().getWorth(key);
        if (worth == null) return 0;

        return worth.getQuantity();
    }

    @Override
    public String getPrice(Player player, ItemStack item, int amount) {
        if (!this.isHooked()) return null;
        String key = ItemUtil.toMaterialKey(item);

        Worth worth = instance.getWorthManager().getWorth(key);
        if (worth == null) return null;

        return Digital.formatDouble("###,###.##", (worth.getPrice() / worth.getQuantity() * amount));
    }

    @Override
    public void sellItem(Player player, ItemStack item, int amount, Consumer<Result> result) {
        if (!this.isHooked()) {
            result.accept(new Result(-1, -1, false));
            return;
        }
        String key = ItemUtil.toMaterialKey(item);

        Worth worth = instance.getWorthManager().getWorth(key);
        if (worth == null) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        int quantity = worth.getQuantity();
        double price = (worth.getPrice() / quantity * amount);

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, key, amount, price);
        }

        result.accept(new Result(amount, price, econ.depositPlayer(player, price).transactionSuccess()));
    }

}
