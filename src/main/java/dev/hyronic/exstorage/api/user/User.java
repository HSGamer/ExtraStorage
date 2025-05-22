package dev.hyronic.exstorage.api.user;

import dev.hyronic.exstorage.api.storage.Storage;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public interface User {

    OfflinePlayer getOfflinePlayer();

    Player getPlayer();

    UUID getUUID();

    String getName();

    /**
     * Check if the player is online
     *
     * @return true if the player is online, otherwise false
     */
    boolean isOnline();

    /**
     * Save the player data to the database.
     */
    void save();

    /**
     * Check if the player has the specified permission or not.
     *
     * @param permission the permission to be checked
     * @return true if the player has permission, otherwise false
     */
    boolean hasPermission(String permission);

    /**
     * Get the player texture
     *
     * @return the player texture
     */
    String getTexture();

    /**
     * Get the storage of the player
     *
     * @return the storage
     */
    Storage getStorage();

    /**
     * Get the partner list
     *
     * @return the list of partners
     */
    Collection<Partner> getPartners();

    /**
     * Check if the specified player is a partner or not
     *
     * @param player UUID of the player to be checked
     * @return true if the specified player is a partner, otherwise false
     */
    boolean isPartner(UUID player);

    /**
     * Add the player to the partner list
     *
     * @param player UUID of the player to be added
     */
    void addPartner(UUID player);

    /**
     * Remove the player from the partner list
     *
     * @param player UUID of the player to be removed
     */
    void removePartner(UUID player);

    /**
     * Cleanup the partner list
     */
    void clearPartners();

}
