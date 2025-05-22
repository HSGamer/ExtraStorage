package me.hsgamer.extrastorage.listeners;

import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.event.Listener;

public abstract class BaseListener
        implements Listener {

    protected final ExtraStorage instance;

    protected BaseListener(ExtraStorage instance) {
        this.instance = instance;
        this.register();
    }

    protected void register() {
        instance.getServer().getPluginManager().registerEvents(this, instance);
    }

}
