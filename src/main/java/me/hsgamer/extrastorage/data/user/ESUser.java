package me.hsgamer.extrastorage.data.user;

import com.google.gson.JsonObject;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.fetcher.TextureFetcher;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ESUser
        implements User {

    private static final ExtraStorage instance = ExtraStorage.getInstance();

    private final OfflinePlayer player;
    private final Map<UUID, Partner> partners;
    private final Storage storage;
    private String texture = "";

    // Sử dụng hàm này khi và chỉ khi tải dữ liệu đã có sẵn của người chơi từ database:
    ESUser(UUID uuid, boolean status, String texture, long space, JsonObject items, JsonObject unused, JsonObject partners) {
        this.player = Bukkit.getServer().getOfflinePlayer(uuid);
        this.partners = new ConcurrentHashMap<>();
        this.storage = new EStorage(this, status, space, items, unused);
        this.texture = texture;

        partners.entrySet().forEach(entry -> {
            UUID partnerId = UUID.fromString(entry.getKey());
            if (this.partners.containsKey(partnerId)) return;

            long timestamp = entry.getValue().getAsLong();
            this.partners.put(partnerId, new ESPartner(partnerId, timestamp));
        });
    }

    // Sử dụng hàm này khi và chỉ khi dữ liệu của người chơi chưa có và phải tạo mới:

    /**
     * Use this constructor if you want to create data for the player who is joining the server for the first time.
     * <p><i>DO NOT USE THIS CONSTRUCTOR FOR LOADING DATA</i></p>
     *
     * @param uuid UUID of the player to be created
     */
    public ESUser(UUID uuid) {
        this.player = Bukkit.getServer().getOfflinePlayer(uuid);
        this.partners = new ConcurrentHashMap<>();
        this.storage = new EStorage(this);

        instance.getUserManager().insert(this);

        TextureFetcher.getTextureUrl(player.getName(), textureUrl -> {
            byte[] texture = ("{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}").getBytes();
            this.texture = new String(Base64.getEncoder().encode(texture));
        });
    }

    @Override
    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    @Override
    public Player getPlayer() {
        return player.getPlayer();
    }

    @Override
    public UUID getUUID() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public void save() {
        String query = "UPDATE `{table}` SET `status`=?, `texture`=?, `space`=?, `partners`=?, `filter`=?, `unfilter`=? WHERE `uuid`=?"
                .replaceAll(Utils.getRegex("table"), instance.getSetting().getDBTable());
        try (Connection conn = instance.getDatabaseClient().getConnection(); PreparedStatement prepare = conn.prepareStatement(query)) {
            prepare.setBoolean(1, storage.getStatus());
            prepare.setString(2, texture);
            prepare.setLong(3, ((EStorage) storage).space);

            JsonObject partners = new JsonObject();
            this.partners.forEach((uuid, partner) -> partners.addProperty(uuid.toString(), partner.getTimestamp()));
            prepare.setString(4, partners.toString());

            JsonObject filter = new JsonObject();
            storage.getFilteredItems().forEach((key, item) -> filter.addProperty(key, item.getQuantity()));
            prepare.setString(5, filter.toString());

            JsonObject unfilter = new JsonObject();
            storage.getUnfilteredItems().forEach((key, item) -> unfilter.addProperty(key, item.getQuantity()));
            prepare.setString(6, unfilter.toString());

            prepare.setString(7, this.getUUID().toString());

            prepare.execute();
        } catch (SQLException error) {
            instance.getLogger().log(Level.SEVERE, "Failed to save data of player " + this.getUUID() + " (" + this.getName() + "). Please contact the author for help!", error);
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        Player player = this.player.getPlayer();
        if ((player == null) || (!player.isOnline())) return false;
        return (player.isOp() || player.hasPermission(permission));
    }

    @Override
    public String getTexture() {
        return texture;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }


    @Override
    public Collection<Partner> getPartners() {
        return partners.values();
    }

    @Override
    public boolean isPartner(UUID player) {
        return partners.containsKey(player);
    }

    @Override
    public void addPartner(UUID player) {
        partners.put(player, new ESPartner(player, System.currentTimeMillis()));
    }

    @Override
    public void removePartner(UUID player) {
        partners.remove(player);
    }

    @Override
    public void clearPartners() {
        partners.clear();
    }

}
