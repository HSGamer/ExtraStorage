package dev.hyronic.exstorage.listeners;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.gui.abstraction.GuiCreator;
import dev.hyronic.exstorage.gui.events.GuiClickEvent;
import dev.hyronic.exstorage.gui.icon.Icon;
import dev.hyronic.exstorage.gui.icon.events.IconClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InventoryListener
        extends BaseListener {

    private final Map<UUID, Double> delayed;

    public InventoryListener(ExtraStorage instance) {
        super(instance);
        this.delayed = new HashMap<>();
    }

    private boolean isDelayed(Player player) {
        double current = System.currentTimeMillis() / 1000.0;
        Double last = delayed.get(player.getUniqueId());
        if ((last != null) && (current < last)) return true;
        delayed.put(player.getUniqueId(), current + 0.15);
        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof GuiCreator)) return;
        GuiCreator gui = (GuiCreator) holder;
        event.setCancelled(true);

        if (this.isDelayed(player)) return;

        GuiClickEvent clickEvent = new GuiClickEvent(event, gui, player);
        gui.callClick(clickEvent);
        if (clickEvent.isCancelled()) return;

        Icon icon = gui.getIconAt(event.getSlot());
        if (icon == null) return;
        icon.callClick(new IconClickEvent(event, icon, player));
    }

}
