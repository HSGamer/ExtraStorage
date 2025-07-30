package me.hsgamer.extrastorage.hooks.economy;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.objects.ShopItem;
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

public final class EconomyShopGuiHook
        implements EconomyProvider {

    private final Economy econ;
    private boolean isPaid;

    public EconomyShopGuiHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        econ = (rsp != null) ? rsp.getProvider() : null;

        if (this.isHooked()) {
            isPaid = instance.getServer().getPluginManager().isPluginEnabled("EconomyShopGUI-Premium");
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
    public String getPrice(Player player, ItemStack item, int amount) {
        if (!this.isHooked()) return null;

        ShopItem shopItem = EconomyShopGUIHook.getShopItem(item);
        if (shopItem == null) return null;

        ItemStack clone = item.clone();
        clone.setAmount(amount);

        Double price = EconomyShopGUIHook.getItemSellPrice(shopItem, clone);
        if (price == null) return null;

        return Digital.formatDouble("###,###.##", price);
    }

    @Override
    public void sellItem(Player player, ItemStack item, int amount, Consumer<Result> result) {
        if (!this.isHooked()) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        ShopItem shopItem = EconomyShopGUIHook.getShopItem(item);
        if (shopItem == null) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        ItemStack clone = item.clone();
        clone.setAmount(amount);

        Double price = EconomyShopGUIHook.getItemSellPrice(shopItem, clone);
        if (price == null) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        if (isPaid) {
            EconomyShopGUIHook.sellItem(shopItem, amount);
        }

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, ItemUtil.toMaterialKey(item), amount, price);
        }

        result.accept(new Result(amount, price, econ.depositPlayer(player, price).transactionSuccess()));
    }

    public boolean isPaid() {
        return this.isPaid;
    }
}
