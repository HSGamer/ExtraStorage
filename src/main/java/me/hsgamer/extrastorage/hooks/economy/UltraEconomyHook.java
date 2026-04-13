package me.hsgamer.extrastorage.hooks.economy;

import me.TechsCode.UltraEconomy.UltraEconomy;
import me.TechsCode.UltraEconomy.UltraEconomyAPI;
import me.TechsCode.UltraEconomy.objects.Account;
import me.TechsCode.UltraEconomy.objects.Currency;
import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class UltraEconomyHook extends WorthEconomyHook {

    private final UltraEconomyAPI api;

    public UltraEconomyHook() {
        this.api = UltraEconomy.getAPI();

        if (this.isHooked()) {
            instance.getLogger().info("Using UltraEconomy as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "UltraEconomy"));
        } else
            instance.getLogger().severe("Could not find dependency: UltraEconomy. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        return (api != null);
    }

    @Override
    protected boolean deposit(Player player, double price) {
        Optional<Account> optional = api.getAccounts().uuid(player.getUniqueId());
        if (!optional.isPresent()) {
            return false;
        }

        String cur = instance.getSetting().getCurrency();
        Currency currency;
        if (!cur.isEmpty()) {
            Optional<Currency> curOptional = api.getCurrencies().name(cur);
            if (!curOptional.isPresent()) {
                return false;
            }
            currency = curOptional.get();
        } else currency = api.getCurrencies().get(0);

        Account account = optional.get();
        account.getBalance(currency).addHand((float) price);
        return true;
    }
}
