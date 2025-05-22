package dev.hyronic.exstorage.gui.icon.events;

import dev.hyronic.exstorage.gui.icon.Icon;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class IconClickEvent
        extends Event {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final InventoryClickEvent event;
    @Getter
    private final Icon icon;
    @Getter
    private final Player player;

    public IconClickEvent(InventoryClickEvent event, Icon icon, Player player) {
        this.event = event;
        this.icon = icon;
        this.player = player;
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

    public static HandlerList getHanderList() {
        return handlers;
    }

}