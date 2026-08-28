package me.hsgamer.extrastorage.util;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.FilterGUI;
import me.hsgamer.extrastorage.gui.PartnerGUI;
import me.hsgamer.extrastorage.gui.SellGUI;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GuiUtil {
    private static final List<GuiEntry> GUI_SEQUENCE = new ArrayList<>();

    static {
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_OPEN_PERMISSION,
                p -> ExtraStorage.getInstance().get(StorageGUI.class).openFor(p, null)));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_SELL_PERMISSION,
                p -> ExtraStorage.getInstance().get(SellGUI.class).openFor(p)));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_PARTNER_PERMISSION,
                p -> ExtraStorage.getInstance().get(PartnerGUI.class).openFor(p)));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_FILTER_PERMISSION,
                p -> ExtraStorage.getInstance().get(FilterGUI.class).openFor(p)));
    }

    private GuiUtil() {
    }

    public static void browseGUI(Player player, BaseGUI<?, ?, ?> current, boolean forward) {
        int currentIndex = findIndex(current);
        if (currentIndex == -1) return;

        int size = GUI_SEQUENCE.size();
        for (int i = 1; i < size; i++) {
            int nextIndex = (currentIndex + (forward ? i : -i) + size) % size;
            GuiEntry entry = GUI_SEQUENCE.get(nextIndex);

            if (player.isOp() || player.hasPermission(entry.permission)) {
                entry.opener.accept(player);
                return;
            }
        }
    }

    private static int findIndex(BaseGUI<?, ?, ?> current) {
        if (current == ExtraStorage.getInstance().get(StorageGUI.class)) return 0;
        if (current == ExtraStorage.getInstance().get(SellGUI.class)) return 1;
        if (current == ExtraStorage.getInstance().get(PartnerGUI.class)) return 2;
        if (current == ExtraStorage.getInstance().get(FilterGUI.class)) return 3;
        return -1;
    }

    private static class GuiEntry {
        private final String permission;
        private final Consumer<Player> opener;

        private GuiEntry(String permission, Consumer<Player> opener) {
            this.permission = permission;
            this.opener = opener;
        }
    }
}
