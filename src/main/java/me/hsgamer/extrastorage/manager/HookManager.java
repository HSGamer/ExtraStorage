package me.hsgamer.extrastorage.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.component.Reloadable;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.hook.economy.*;
import me.hsgamer.extrastorage.hook.placeholder.ESPlaceholder;
import me.hsgamer.extrastorage.util.SoundUtil;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class HookManager implements Loadable, Reloadable {
    private static final Map<String, Function<ExtraStorage, EconomyProvider>> ECONOMY_PROVIDERS = new LinkedHashMap<>();

    static {
        ECONOMY_PROVIDERS.put("SHOPGUIPLUS", ShopGuiPlusHook::new);
        ECONOMY_PROVIDERS.put("ECONOMYSHOPGUI", EconomyShopGuiHook::new);
        ECONOMY_PROVIDERS.put("PLAYERPOINTS", PlayerPointsHook::new);
        ECONOMY_PROVIDERS.put("TOKENMANAGER", TokenManagerHook::new);
        ECONOMY_PROVIDERS.put("ULTRAECONOMY", UltraEconomyHook::new);
        ECONOMY_PROVIDERS.put("COINSENGINE", ExcellentEconomyHook::new);
        ECONOMY_PROVIDERS.put("EXCELLENTECONOMY", ExcellentEconomyHook::new);
        ECONOMY_PROVIDERS.put("VAULT", VaultHook::new);
    }

    private final ExtraStorage plugin;
    private EconomyProvider economyProvider;
    private Consumer<Player> pickupSoundPlayer;
    private ESPlaceholder placeholder;

    public HookManager(ExtraStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        reload();
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholder = new ESPlaceholder(plugin);
            if (placeholder.register()) {
                plugin.getLogger().info("Hooked into PlaceholderAPI");
            }
        }
    }

    @Override
    public void disable() {
        if (placeholder != null) {
            if (placeholder.isRegistered()) {
                placeholder.unregister();
            }
            this.placeholder = null;
        }
    }

    @Override
    public void reload() {
        SettingConfig setting = plugin.get(SettingConfig.class);
        this.economyProvider = resolveEconomyProvider(setting);
        this.pickupSoundPlayer = SoundUtil.getSoundPlayer(setting.pickupSound());
    }

    private EconomyProvider resolveEconomyProvider(SettingConfig setting) {
        String provider = setting.economy().provider().toUpperCase();
        EconomyProvider hook = ECONOMY_PROVIDERS.getOrDefault(provider, NoneEconomyHook::new).apply(plugin);
        if (!hook.isHooked()) {
            plugin.getLogger().warning("Economy provider '" + provider + "' not available. Selling is disabled.");
            return new NoneEconomyHook(plugin);
        }
        plugin.getLogger().info("Using " + provider + " as economy provider.");
        return hook;
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public Consumer<Player> getPickupSoundPlayer() {
        return pickupSoundPlayer;
    }
}
