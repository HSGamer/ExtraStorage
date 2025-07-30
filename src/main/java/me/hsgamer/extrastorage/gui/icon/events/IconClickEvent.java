package me.hsgamer.extrastorage.gui.icon.events;

import me.hsgamer.extrastorage.gui.icon.Icon;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class IconClickEvent
        extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final InventoryClickEvent event;
    private final Icon icon;
    private final Player player;

    public IconClickEvent(InventoryClickEvent event, Icon icon, Player player) {
        this.event = event;
        this.icon = icon;
        this.player = player;
    }

    public static HandlerList getHanderList() {
        return handlers;
    }

    public boolean isShiftClick() {
        return event.isShiftClick();
    }

    public boolean isRightClick() {
        return event.isRightClick();
    }

    public boolean isLeftClick() {
        return event.isLeftClick();
    }

    public ItemStack getClickedItem() {
        return event.getCurrentItem();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public InventoryClickEvent getEvent() {
        return this.event;
    }

    public Icon getIcon() {
        return this.icon;
    }

    public Player getPlayer() {
        return this.player;
    }
}