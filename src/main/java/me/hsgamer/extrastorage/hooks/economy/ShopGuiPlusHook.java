package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class ShopGuiPlusHook
        implements EconomyProvider {

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

        try {
            Class<ShopGuiPlusApi> clazz = ShopGuiPlusApi.class;

            Method method = clazz.getMethod("getItemStackShopItem", ItemStack.class);
            Object obj = method.invoke(clazz, item);

            Class<?> objClazz = obj.getClass();
            Method getItem = objClazz.getMethod("getItem");

            return ((ItemStack) getItem.invoke(obj)).getAmount();
        } catch (NoSuchMethodException error) {
            error.printStackTrace();
        } catch (IllegalAccessException error) {
            error.printStackTrace();
        } catch (InvocationTargetException error) {
            error.printStackTrace();
        }

        return 0;
    }

    @Override
    public String getPrice(Player player, ItemStack item, int amount) {
        if ((!this.isHooked()) || (ShopGuiPlusApi.getItemStackPriceSell(item) == -1)) {
            return null;
        }

        try {
            Class<ShopGuiPlusApi> clazz = ShopGuiPlusApi.class;

            Method method = clazz.getMethod("getItemStackShopItem", ItemStack.class);
            ShopItem shopItem = (ShopItem) method.invoke(clazz, item);

            Method modMethod = clazz.getMethod("getPriceModifier", Player.class, ShopItem.class, PriceModifierActionType.class);
            PriceModifier priceMod = (PriceModifier) modMethod.invoke(clazz, player, shopItem, PriceModifierActionType.SELL);

            Method sellPriceForAmount = shopItem.getClass().getMethod("getSellPriceForAmount", int.class);
            double price = (double) sellPriceForAmount.invoke(shopItem, amount);

            if ((priceMod != null) && (priceMod.getModifier() > 1.0)) price *= priceMod.getModifier();

            return Digital.formatDouble("###,###.##", price);
        } catch (NoSuchMethodException error) {
            error.printStackTrace();
        } catch (IllegalAccessException error) {
            error.printStackTrace();
        } catch (InvocationTargetException error) {
            error.printStackTrace();
        }

        return null;
    }

    @Override
    public void sellItem(Player player, ItemStack item, int amount, Consumer<Result> result) {
        if ((!this.isHooked()) || (ShopGuiPlusApi.getItemStackPriceSell(item) == -1)) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        try {
            Class<ShopGuiPlusApi> clazz = ShopGuiPlusApi.class;

            Method method = clazz.getMethod("getItemStackShopItem", Player.class, ItemStack.class);
            ShopItem shopItem = (ShopItem) method.invoke(clazz, player, item);

            Method modMethod = clazz.getMethod("getPriceModifier", Player.class, ShopItem.class, PriceModifierActionType.class);
            PriceModifier priceMod = (PriceModifier) modMethod.invoke(clazz, player, shopItem, PriceModifierActionType.SELL);

            Method sellPriceForAmount = shopItem.getClass().getMethod("getSellPriceForAmount", int.class);
            double price = (double) sellPriceForAmount.invoke(shopItem, amount);

            if ((priceMod != null) && (priceMod.getModifier() > 1.0)) price *= priceMod.getModifier();

            if (instance.getSetting().isLogSales()) {
                instance.getLog().log(player, null, Log.Action.SELL, ItemUtil.toMaterialKey(item), amount, price);
            }

            result.accept(new Result(amount, price, econ.depositPlayer(player, price).transactionSuccess()));
        } catch (NoSuchMethodException error) {
            error.printStackTrace();
        } catch (IllegalAccessException error) {
            error.printStackTrace();
        } catch (InvocationTargetException error) {
            error.printStackTrace();
        }
    }

}
