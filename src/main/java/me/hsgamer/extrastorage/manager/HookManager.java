package me.hsgamer.extrastorage.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.component.Reloadable;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.hook.economy.*;
import me.hsgamer.extrastorage.hook.placeholder.ESPlaceholder;
import me.hsgamer.extrastorage.util.SoundUtil;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class HookManager implements Loadable, Reloadable {
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
        EconomyProvider hook;
        switch (provider) {
            case "SHOPGUIPLUS":
                hook = new ShopGuiPlusHook(plugin);
                break;
            case "ECONOMYSHOPGUI":
                hook = new EconomyShopGuiHook(plugin);
                break;
            case "PLAYERPOINTS":
                hook = new PlayerPointsHook(plugin);
                break;
            case "TOKENMANAGER":
                hook = new TokenManagerHook(plugin);
                break;
            case "ULTRAECONOMY":
                hook = new UltraEconomyHook(plugin);
                break;
            case "COINSENGINE":
            case "EXCELLENTECONOMY":
                hook = new ExcellentEconomyHook(plugin);
                break;
            case "VAULT":
                hook = new VaultHook(plugin);
                break;
            default:
                hook = new NoneEconomyHook(plugin);
                break;
        }
        if (!hook.isHooked()) {
            hook = new NoneEconomyHook(plugin);
        }
        return hook;
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public Consumer<Player> getPickupSoundPlayer() {
        return pickupSoundPlayer;
    }
}
