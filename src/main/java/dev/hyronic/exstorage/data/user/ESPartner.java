package dev.hyronic.exstorage.data.user;

import dev.hyronic.exstorage.ExtraStorage;
import dev.hyronic.exstorage.api.user.Partner;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class ESPartner
        implements Partner {

    private final OfflinePlayer player;

    @Override
    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    private final long timestamp;

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getTimeFormatted() {
        return DateFormatUtils.format(timestamp, ExtraStorage.getInstance().getSetting().getDateFormat());
    }


    ESPartner(UUID uuid, long timestamp) {
        this.player = Bukkit.getServer().getOfflinePlayer(uuid);
        this.timestamp = timestamp;
    }

}
