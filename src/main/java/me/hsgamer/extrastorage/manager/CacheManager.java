package me.hsgamer.extrastorage.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.component.Reloadable;
import me.hsgamer.extrastorage.config.SettingConfig;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CacheManager implements Loadable, Reloadable {
    private final ExtraStorage instance;
    private volatile Set<String> blacklist = Collections.emptySet();
    private volatile Set<String> whitelist = Collections.emptySet();
    private volatile Set<String> blacklistWorlds = Collections.emptySet();
    private volatile Consumer<Player> pickupSoundPlayer;

    public CacheManager(ExtraStorage instance) {
        this.instance = instance;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void reload() {
        this.blacklist = new HashSet<>(instance.get(SettingConfig.class).getNormalizedBlacklist());
        this.whitelist = new HashSet<>(instance.get(SettingConfig.class).getNormalizedWhitelist());
        this.blacklistWorlds = new HashSet<>(instance.get(SettingConfig.class).blacklistWorlds());
        this.pickupSoundPlayer = instance.get(HookManager.class).getPickupSoundPlayer();
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }

    public boolean isBlacklistedWorld(World world) {
        if (world == null) return true;
        return blacklistWorlds.contains(world.getName());
    }

    public Consumer<Player> getPickupSoundPlayer() {
        return pickupSoundPlayer;
    }
}
