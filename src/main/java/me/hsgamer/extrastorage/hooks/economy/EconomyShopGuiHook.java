package me.hsgamer.extrastorage.hooks.economy;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.objects.ShopItem;
import net.milkbowl.vault.economy.Economy;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyShopGuiHook extends AbstractEconomyHook {

    private final Economy econ;
    private final boolean isPaid;

    public EconomyShopGuiHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        econ = (rsp != null) ? rsp.getProvider() : null;
        isPaid = instance.getServer().getPluginManager().isPluginEnabled("EconomyShopGUI-Premium");

        if (this.isHooked()) {
            instance.getLogger().info("Using EconomyShopGUI (" + (isPaid ? "paid" : "free") + " version) as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "EconomyShopGUI"));
        } else
            instance.getLogger().severe("Could not find dependency: EconomyShopGUI (free or paid version). Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        return (instance.getServer().getPluginManager().isPluginEnabled("EconomyShopGUI") || instance.getServer().getPluginManager().isPluginEnabled("EconomyShopGUI-Premium"));
    }

    @Override
    public int getAmount(ItemStack item) {
        if (!this.isHooked()) return 0;

        ShopItem shopItem = EconomyShopGUIHook.getShopItem(item);
        if (shopItem == null) return 0;

        return shopItem.getStackSize();
    }

    @Override
    protected double getRawPrice(Player player, ItemStack item, int amount) {
        ShopItem shopItem = EconomyShopGUIHook.getShopItem(item);
        if (shopItem == null) return -1;

        ItemStack clone = item.clone();
        clone.setAmount(amount);

        Double price = EconomyShopGUIHook.getItemSellPrice(shopItem, clone);
        return price != null ? price : -1;
    }

    @Override
    protected boolean deposit(Player player, ItemStack item, int amount, double price) {
        if (isPaid) {
            ShopItem shopItem = EconomyShopGUIHook.getShopItem(item);
            if (shopItem != null) {
                EconomyShopGUIHook.sellItem(shopItem, amount);
            }
        }
        return econ.depositPlayer(player, price).transactionSuccess();
    }
}
