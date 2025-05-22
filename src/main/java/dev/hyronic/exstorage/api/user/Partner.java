package dev.hyronic.exstorage.api.user;

import org.bukkit.OfflinePlayer;

public interface Partner {

    OfflinePlayer getOfflinePlayer();

    long getTimestamp();

    String getTimeFormatted();

}
