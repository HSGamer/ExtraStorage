package me.hsgamer.extrastorage.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class BaseEvent
        extends Event {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
