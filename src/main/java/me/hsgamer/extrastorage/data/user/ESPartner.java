package me.hsgamer.extrastorage.data.user;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.Partner;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class ESPartner
        implements Partner {

    private final OfflinePlayer player;
    private final long timestamp;

    ESPartner(UUID uuid, long timestamp) {
        this.player = Bukkit.getServer().getOfflinePlayer(uuid);
        this.timestamp = timestamp;
    }

    @Override
    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getTimeFormatted() {
        return DateFormatUtils.format(timestamp, ExtraStorage.getInstance().getSetting().getDateFormat());
    }

}
