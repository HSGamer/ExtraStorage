package dev.hyronic.exstorage.gui.events;

import dev.hyronic.exstorage.gui.abstraction.GuiCreator;
import lombok.Getter;
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHanderList() {
        return handlers;
    }

}
