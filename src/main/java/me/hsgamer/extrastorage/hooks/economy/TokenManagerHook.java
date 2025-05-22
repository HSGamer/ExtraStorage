package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.realized.tokenmanager.api.TokenManager;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class TokenManagerHook
        implements EconomyProvider {

    private final TokenManager api;

    public TokenManagerHook() {
        Plugin tmPlugin = Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
        api = (tmPlugin != null) ? (TokenManager) tmPlugin : null;

        if (this.isHooked()) {
            instance.getLogger().info("Using TokenManager as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "TokenManager"));
        } else
            instance.getLogger().severe("Could not find dependency: TokenManager. Please install it then try again!");
    }

    public static String getUserId() {
        return "%%__USER__%%";
    }

    public static String getResourceId() {
        return "%%__RESOURCE__%%";
    }

    public static String getUniqueId() {
        return "%%__NONCE__%%";
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
        long price = (long) (worth.getPrice() / worth.getQuantity() * amount);

        return Digital.formatThousands(price);
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

        int quantity = worth.getQuantity();
        long price = (long) (worth.getPrice() / quantity * amount);

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, key, amount, price);
        }

        result.accept(new Result(amount, price, api.addTokens(player, price)));
    }

}
