package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import java.util.Optional;
import java.util.function.Consumer;

public final class CoinsEngineHook
        implements EconomyProvider {

    public CoinsEngineHook() {
        if (this.isHooked()) {
            instance.getLogger().info("Using CoinsEngine as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "CoinsEngine"));
        } else instance.getLogger().severe("Could not find dependency: CoinsEngine. Please install it then try again!");
    }

    @Override
    public boolean isHooked() {
        return instance.getServer().getPluginManager().isPluginEnabled("CoinsEngine");
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

        String cur = instance.getSetting().getCurrency();
        Currency currency;

        if (CoinsEngineAPI.hasCurrency(cur)) currency = CoinsEngineAPI.getCurrency(cur);
        else {
            instance.getLogger().warning("The currency with ID '" + cur + "' could not be found! Using Vault as default!");
            Optional<Currency> optional = CoinsEngineAPI.getCurrencyManager().getVaultCurrency();
            if (!optional.isPresent()) {
                result.accept(new Result(-1, -1, false));
                return;
            }
            currency = optional.get();
        }

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, key, amount, price);
        }

        CoinsEngineAPI.addBalance(player, currency, price);
        result.accept(new Result(amount, price, true));
    }

}
