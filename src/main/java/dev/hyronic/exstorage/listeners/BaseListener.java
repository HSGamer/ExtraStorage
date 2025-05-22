package dev.hyronic.exstorage.listeners;

import dev.hyronic.exstorage.ExtraStorage;
import org.bukkit.event.Listener;

public abstract class BaseListener
        implements Listener {

    protected final ExtraStorage instance;

    protected BaseListener(ExtraStorage instance) {
        this.instance = instance;
        this.register();
    }

    protected void register() {
        instance.getPlugMan().registerEvents(this, instance);
    }

}
