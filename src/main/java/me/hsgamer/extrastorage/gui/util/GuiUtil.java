package me.hsgamer.extrastorage.gui.util;

import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.FilterGUI;
import me.hsgamer.extrastorage.gui.PartnerGUI;
import me.hsgamer.extrastorage.gui.SellGUI;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class GuiUtil {
    private static final List<GuiEntry> GUI_SEQUENCE = new ArrayList<>();

    static {
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_OPEN_PERMISSION, p -> new StorageGUI(p, null), StorageGUI.class));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_SELL_PERMISSION, SellGUI::new, SellGUI.class));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_PARTNER_PERMISSION, PartnerGUI::new, PartnerGUI.class));
        GUI_SEQUENCE.add(new GuiEntry(Constants.PLAYER_FILTER_PERMISSION, FilterGUI::new, FilterGUI.class));
    }

    private GuiUtil() {
    }

    public static void browseGUI(Player player, BaseGUI<?> current, boolean forward) {
        int currentIndex = -1;
        for (int i = 0; i < GUI_SEQUENCE.size(); i++) {
            if (GUI_SEQUENCE.get(i).guiClass.isInstance(current)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) return;

        int size = GUI_SEQUENCE.size();
        for (int i = 1; i < size; i++) {
            int nextIndex = (currentIndex + (forward ? i : -i) + size) % size;
            GuiEntry entry = GUI_SEQUENCE.get(nextIndex);

            if (player.isOp() || player.hasPermission(entry.permission)) {
                entry.opener.apply(player).open();
                return;
            }
        }
    }

    private static class GuiEntry {
        private final String permission;
        private final Function<Player, BaseGUI<?>> opener;
        private final Class<? extends BaseGUI<?>> guiClass;

        private GuiEntry(String permission, Function<Player, BaseGUI<?>> opener, Class<? extends BaseGUI<?>> guiClass) {
            this.permission = permission;
            this.opener = opener;
            this.guiClass = guiClass;
        }
    }
}
