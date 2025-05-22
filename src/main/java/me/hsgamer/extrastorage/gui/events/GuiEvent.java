package me.hsgamer.extrastorage.gui.events;

import lombok.Getter;
import me.hsgamer.extrastorage.gui.abstraction.GuiCreator;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class GuiEvent
        extends Event {

    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final GuiCreator gui;

    public GuiEvent(GuiCreator gui) {
        this.gui = gui;
    }

    public static HandlerList getHanderList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
