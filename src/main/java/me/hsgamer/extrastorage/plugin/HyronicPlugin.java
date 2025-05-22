package me.hsgamer.extrastorage.plugin;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public abstract class HyronicPlugin
        extends JavaPlugin {

    @Getter
    protected static HyronicPlugin instance;
    @Getter
    protected PluginManager plugMan;
    protected Logger logger;

    /**
     * Called when the plugin is loading.
     */
    public void load() {
    }

    /**
     * Called when the plugin is enabling.
     */
    public void enable() {
    }

    /**
     * Called when the plugin is disabling.
     */
    public void disable() {
    }

    @Override
    public final void onLoad() {
        instance = this;
        this.plugMan = this.getServer().getPluginManager();
        this.logger = this.getLogger();
        this.load();
    }

    @Override
    public final void onEnable() {
        this.enable();
        logger.info("Plugin loaded successfully!");
    }

    @Override
    public final void onDisable() {
        this.disable();
        logger.info("Plugin disabled successfully!");
    }

    public final boolean isHooked(String plName) {
        return ((plugMan.getPlugin(plName) != null) && plugMan.isPluginEnabled(plName));
    }

}
