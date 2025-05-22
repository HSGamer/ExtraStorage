package me.hsgamer.extrastorage.gui.abstraction;

import me.hsgamer.extrastorage.gui.events.GuiClickEvent;

import java.util.function.Consumer;

public interface GuiAction {

    void callClick(GuiClickEvent event);

    void handleClick(Consumer<GuiClickEvent> handler);

}
