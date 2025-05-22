package me.hsgamer.extrastorage.gui.icon;


import me.hsgamer.extrastorage.gui.icon.events.IconClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class Icon {

    private final ItemStack item;
    private Consumer<IconClickEvent> clickHandler;
    private int[] slots;

    public Icon(ItemStack item) {
        this.item = item;
        this.clickHandler = (event) -> {
        };
    }

    public ItemStack getItemStack() {
        return item;
    }

    public void callClick(IconClickEvent event) {
        clickHandler.accept(event);
    }

    public Icon handleClick(Consumer<IconClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    public int[] getSlots() {
        return slots;
    }

    public Icon setSlots(int... slots) {
        this.slots = slots;
        return this;
    }

}