package me.hsgamer.extrastorage.hooks.economy;

import me.TechsCode.UltraEconomy.UltraEconomy;
import me.TechsCode.UltraEconomy.UltraEconomyAPI;
import me.TechsCode.UltraEconomy.objects.Account;
import me.TechsCode.UltraEconomy.objects.Currency;
import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Consumer;

public final class UltraEconomyHook
        implements EconomyProvider {

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
        double price = (worth.getPrice() / worth.getQuantity() * amount);

        Optional<Account> optional = api.getAccounts().uuid(player.getUniqueId());
        if (!optional.isPresent()) {
            result.accept(new Result(-1, -1, false));
            return;
        }

        String cur = instance.getSetting().getCurrency();
        Currency currency;
        if (!cur.isEmpty()) {
            Optional<Currency> curOptional = api.getCurrencies().name(cur);
            if (!curOptional.isPresent()) {
                result.accept(new Result(-1, -1, false));
                return;
            }
            currency = curOptional.get();
        } else currency = api.getCurrencies().get(0);

        Account account = optional.get();
        account.getBalance(currency).addHand((float) price);

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, key, amount, price);
        }

        result.accept(new Result(amount, price, true));
    }

}
