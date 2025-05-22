package me.hsgamer.extrastorage.hooks.economy;

import me.hsgamer.extrastorage.api.item.Worth;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class PlayerPointsHook
        implements EconomyProvider {

    private final PlayerPointsAPI api;

    public PlayerPointsHook() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PlayerPoints");
        api = (plugin != null) ? ((PlayerPoints) plugin).getAPI() : null;

        if (this.isHooked()) {
            instance.getLogger().info("Using PlayerPoints as economy provider.");
            instance.getMetrics().addCustomChart(new SimplePie("economy_provider", () -> "PlayerPoints"));
        } else
            instance.getLogger().severe("Could not find dependency: PlayerPoints. Please install it then try again!");
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
        int price = (int) (worth.getPrice() / worth.getQuantity() * amount);

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
        int quantity = worth.getQuantity(), price = (int) (worth.getPrice() / quantity * amount);

        if (instance.getSetting().isLogSales()) {
            instance.getLog().log(player, null, Log.Action.SELL, key, amount, price);
        }

        result.accept(new Result(amount, price, api.give(player.getUniqueId(), price)));
    }

}
