package me.hsgamer.extrastorage.gui.events;

import me.hsgamer.extrastorage.gui.abstraction.GuiCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class GuiClickEvent
        extends GuiEvent
        implements Cancellable {

    private final InventoryClickEvent event;
    private final Player player;
    private boolean cancelled = false;

    public GuiClickEvent(InventoryClickEvent event, GuiCreator gui, Player player) {
        super(gui);
        this.event = event;
        this.player = player;
    }

    public boolean isTopClick() {
        return event.getClickedInventory().equals(player.getOpenInventory().getTopInventory());
    }

    public ItemStack getClickedItem() {
        return event.getCurrentItem();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public InventoryClickEvent getEvent() {
        return this.event;
    }

    public Player getPlayer() {
        return this.player;
    }
}
