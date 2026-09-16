package me.hsgamer.extrastorage.hook.economy;

import me.TechsCode.UltraEconomy.UltraEconomy;
import me.TechsCode.UltraEconomy.UltraEconomyAPI;
import me.TechsCode.UltraEconomy.objects.Account;
import me.TechsCode.UltraEconomy.objects.Currency;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.config.SettingConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class UltraEconomyHook extends AbstractEconomyHook {

    private final UltraEconomyAPI api;
    private final Currency currency;
    private final boolean hooked;

    public UltraEconomyHook(ExtraStorage plugin) {
        super(plugin);
        UltraEconomyAPI api = UltraEconomy.getAPI();
        Currency currency = null;
        if (api != null) {
            String cur = plugin.get(SettingConfig.class).economy().currency();
            if (!cur.isEmpty()) {
                currency = api.getCurrencies().name(cur).orElse(null);
            } else if (!api.getCurrencies().isEmpty()) {
                currency = api.getCurrencies().get(0);
            }
        }
        this.api = api;
        this.currency = currency;
        this.hooked = api != null && currency != null;
    }

    @Override
    public boolean isHooked() {
        return hooked;
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
        Optional<Account> optional = api.getAccounts().uuid(player.getUniqueId());
        if (!optional.isPresent()) {
            return false;
        }

        Account account = optional.get();
        account.getBalance(currency).addHand((float) price);
        return true;
    }
}
