package me.hsgamer.extrastorage.data.stub;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.Partner;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class StubPartner implements Partner {
    private final OfflinePlayer player;
    private final long timestamp;

    public StubPartner(UUID uuid, long timestamp) {
        this.player = Bukkit.getOfflinePlayer(uuid);
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
