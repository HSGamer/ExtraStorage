package me.hsgamer.extrastorage.hooks.economy;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.modifier.PriceModifier;
import net.brcdev.shopgui.modifier.PriceModifierActionType;
import net.brcdev.shopgui.shop.item.ShopItem;
import net.milkbowl.vault.economy.Economy;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class ShopGuiPlusHook extends AbstractEconomyHook {

    private boolean setup = false;
    private Economy econ;

    public ShopGuiPlusHook() {
        if (this.isHooked()) {
            instance.getLogger().info("Using ShopGUIPlus as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "ShopGUIPlus"));
        } else instance.getLogger().severe("Could not find dependency: ShopGUIPlus. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        if (Bukkit.getPluginManager().getPlugin("ShopGUIPlus") == null) {
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
        if ((!this.isHooked()) || (ShopGuiPlusApi.getItemStackPriceSell(item) == -1)) {
            return 0;
        }

        ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(item);
        ItemStack shopItemStack = shopItem.getItem();
        return shopItemStack.getAmount();
    }

    @Override
    protected double getRawPrice(Player player, ItemStack item, int amount) {
        if ((!this.isHooked()) || (ShopGuiPlusApi.getItemStackPriceSell(item) == -1)) {
            return -1;
        }

        try {
            ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(item);
            PriceModifier priceMod = ShopGuiPlusApi.getPriceModifier(player, shopItem, PriceModifierActionType.SELL);
            double price = shopItem.getSellPriceForAmount(amount);
            if ((priceMod != null) && (priceMod.getModifier() > 1.0)) price *= priceMod.getModifier();
            return price;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    protected boolean deposit(Player player, ItemStack item, int amount, double price) {
        return econ.depositPlayer(player, price).transactionSuccess();
    }
}
