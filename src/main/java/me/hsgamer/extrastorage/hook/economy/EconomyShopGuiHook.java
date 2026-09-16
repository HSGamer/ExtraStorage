package me.hsgamer.extrastorage.hook.economy;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.hsgamer.extrastorage.ExtraStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class EconomyShopGuiHook extends AbstractEconomyHook {

    private Economy econ;
    private boolean paid;
    private boolean hooked;
    private boolean setup;

    public EconomyShopGuiHook(ExtraStorage plugin) {
        super(plugin);
    }

    @Override
    public boolean isHooked() {
        if (!setup) {
            paid = instance.getServer().getPluginManager().isPluginEnabled("EconomyShopGUI-Premium");
            boolean shopPresent = instance.getServer().getPluginManager().isPluginEnabled("EconomyShopGUI") || paid;
            if (shopPresent && Bukkit.getPluginManager().getPlugin("Vault") != null) {
                econ = findVaultEconomy();
            }
            hooked = shopPresent && econ != null;
            setup = true;
        }
        return hooked;
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
    protected void onBeforeSell(Player player, ItemStack item, int amount, double price) {
        if (paid) {
            ShopItem shopItem = EconomyShopGUIHook.getShopItem(item);
            if (shopItem != null) {
                EconomyShopGUIHook.sellItem(shopItem, amount);
            }
        }
    }

    @Override
    protected boolean deposit(Player player, double price) {
        return econ.depositPlayer(player, price).transactionSuccess();
    }
}
