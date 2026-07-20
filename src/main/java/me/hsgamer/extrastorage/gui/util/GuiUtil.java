package me.hsgamer.extrastorage.gui.util;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GuiUtil {
    private static final List<GuiEntry> GUI_SEQUENCE = new ArrayList<>();

    static {
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_OPEN_PERMISSION,
                p -> ExtraStorage.getInstance().getStorageGUI().openFor(p, null)));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_SELL_PERMISSION,
                p -> ExtraStorage.getInstance().getSellGUI().openFor(p)));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_PARTNER_PERMISSION,
                p -> ExtraStorage.getInstance().getPartnerGUI().openFor(p)));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_FILTER_PERMISSION,
                p -> ExtraStorage.getInstance().getFilterGUI().openFor(p)));
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
        if (current == ExtraStorage.getInstance().getStorageGUI()) return 0;
        if (current == ExtraStorage.getInstance().getSellGUI()) return 1;
        if (current == ExtraStorage.getInstance().getPartnerGUI()) return 2;
        if (current == ExtraStorage.getInstance().getFilterGUI()) return 3;
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
