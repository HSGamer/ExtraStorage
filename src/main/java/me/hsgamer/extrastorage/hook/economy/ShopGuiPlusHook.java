package me.hsgamer.extrastorage.hook.economy;

import me.hsgamer.extrastorage.ExtraStorage;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.modifier.PriceModifier;
import net.brcdev.shopgui.modifier.PriceModifierActionType;
import net.brcdev.shopgui.shop.item.ShopItem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ShopGuiPlusHook extends AbstractEconomyHook {

    private final Economy econ;
    private final boolean hooked;

    public ShopGuiPlusHook(ExtraStorage plugin) {
        super(plugin);
        econ = findVaultEconomy();
        hooked = Bukkit.getPluginManager().getPlugin("ShopGUIPlus") != null && econ != null;
    }

    @Override
    public boolean isHooked() {
        return hooked;
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
    protected boolean deposit(Player player, double price) {
        return econ.depositPlayer(player, price).transactionSuccess();
    }
}
