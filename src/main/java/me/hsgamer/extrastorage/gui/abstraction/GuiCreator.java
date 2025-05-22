package me.hsgamer.extrastorage.gui.abstraction;

import lombok.Getter;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.events.GuiClickEvent;
import me.hsgamer.extrastorage.gui.icon.Icon;
import me.hsgamer.extrastorage.plugin.HyronicPlugin;
import me.hsgamer.extrastorage.util.Digital;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GuiCreator<T extends HyronicPlugin>
        extends GuiConfig
        implements InventoryHolder, GuiAction {

    protected final T instance;
    @Getter
    protected final Player player;
    private final Pattern SLOT_PATTERN;
    private final Map<Integer, Icon> icons;
    protected Inventory inv;
    private Consumer<GuiClickEvent> clickHandler;

    public GuiCreator(String fileName, Player player) {
        super(fileName);

        this.instance = (T) HyronicPlugin.getInstance();
        this.SLOT_PATTERN = Pattern.compile("(?<start>\\d+)-(?<end>\\d+)");

        this.player = player;

        this.icons = new HashMap<>();
        this.clickHandler = (event) -> {
        };

        this.inv = Bukkit.getServer().createInventory(this, rows, title);
    }

    public void open() {
        player.openInventory(inv);
    }

    @Override
    public final Inventory getInventory() {
        return inv;
    }

    @Override
    public final void callClick(GuiClickEvent event) {
        clickHandler.accept(event);
    }

    @Override
    public final void handleClick(Consumer<GuiClickEvent> handler) {
        this.clickHandler = handler;
    }


    public final Icon getIconAt(int slot) {
        return icons.getOrDefault(slot, null);
    }

    public final void addIcon(Icon icon) {
        int[] slots = icon.getSlots();
        if (slots == null) return;
        for (int slot : slots) {
            if ((slot < 1) || (slot > 54)) continue;
            slot -= 1;
            this.inv.setItem(slot, icon.getItemStack());
            this.icons.put(slot, icon);
        }
    }


    protected final void playSoundIfPresent() {
        if (sound != null) player.playSound(player.getLocation(), sound, 4.0f, 1.0f);
    }

    protected final boolean hasPermission(String perm) {
        return (player.isOp() || player.hasPermission(perm));
    }


    protected final int[] getSlots(String path) {
        int slot = config.getInt(path + ".Slot");
        if ((slot > 0) && (slot < 55)) return new int[]{slot};

        List<String> slotList = config.getStringList(path + ".Slots");
        if (slotList.isEmpty()) return null;
        Set<Integer> slotSet = new LinkedHashSet<>();
        for (String slotStr : slotList) {
            Matcher matcher = SLOT_PATTERN.matcher(slotStr);
            try {
                if (!matcher.find()) slotSet.add(Digital.getBetween(1, 54, Integer.parseInt(slotStr)));
                else {
                    int start = Digital.getBetween(1, 54, Integer.parseInt(matcher.group("start")));
                    int end = Digital.getBetween(start, 54, Integer.parseInt(matcher.group("end")));
                    for (int i = start; i <= end; i++) slotSet.add(i);
                }
            } catch (NumberFormatException ignored) {
                // Bỏ qua lỗi nếu giá trị không phải là số.
            }
        }
        int[] slots = new int[slotSet.size()];
        for (int i = 0; i < slotSet.size(); i++) slots[i] = (int) slotSet.toArray()[i];
        return slots;
    }

}
