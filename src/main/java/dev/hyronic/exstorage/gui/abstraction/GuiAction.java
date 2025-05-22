package dev.hyronic.exstorage.gui.abstraction;

import dev.hyronic.exstorage.gui.events.GuiClickEvent;

import java.util.function.Consumer;

public interface GuiAction {

    void callClick(GuiClickEvent event);

    void handleClick(Consumer<GuiClickEvent> handler);

}
