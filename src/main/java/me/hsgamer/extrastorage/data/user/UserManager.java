package me.hsgamer.extrastorage.data.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.events.StorageLoadEvent;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class UserManager {

    private final ExtraStorage instance;
    private final Map<UUID, User> users;
    @Getter
    private boolean loaded;

    public UserManager(ExtraStorage instance) {
        this.instance = instance;
        this.loaded = false;
        this.users = new ConcurrentHashMap<>();
        this.load();
    }

    private void load() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final StorageLoadEvent event = new StorageLoadEvent();

            try (Connection conn = instance.getDatabaseClient().getConnection(); Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM `" + instance.getSetting().getDBTable() + '`');
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    if (users.containsKey(uuid)) continue;

                    boolean status = rs.getBoolean("status");
                    String texture = rs.getString("texture");
                    if (texture == null) texture = "";
                    long space = rs.getLong("space");
                    JsonObject items = new JsonParser().parse(rs.getString("filter")).getAsJsonObject(),
                            unused = new JsonParser().parse(rs.getString("unfilter")).getAsJsonObject(),
                            partners = new JsonParser().parse(rs.getString("partners")).getAsJsonObject();

                    users.put(uuid, new ESUser(uuid, status, texture, space, items, unused, partners));
                }
                rs.close();

                Bukkit.getServer().getOnlinePlayers().forEach(player -> {
                    UUID uuid = player.getUniqueId();
                    if (!users.containsKey(uuid)) new ESUser(uuid);
                });

                event.setLoaded(true);
            } catch (SQLException error) {
                instance.getLogger().log(Level.SEVERE, "Failed to load the user data! Please contact the author for help!", error);
                event.setLoaded(false);
            } finally {
                this.loaded = true;
                Bukkit.getScheduler().runTask(instance, () -> Bukkit.getServer().getPluginManager().callEvent(event));
            }
        });
        executor.shutdown();
    }


    void insert(ESUser user) {
        String query = "INSERT INTO `{table}`(`uuid`, `texture`, `space`, `partners`, `filter`, `unfilter`) VALUES (?, ?, ?, '{}', '{}', '{}')"
                .replaceAll(Utils.getRegex("table"), instance.getSetting().getDBTable());
        try (Connection conn = instance.getDatabaseClient().getConnection(); PreparedStatement prepare = conn.prepareStatement(query)) {
            prepare.setString(1, user.getUUID().toString());
            prepare.setString(2, user.getTexture());
            prepare.setLong(3, instance.getSetting().getMaxSpace());
            prepare.execute();
        } catch (SQLException error) {
            instance.getLogger().log(Level.SEVERE, "Failed to create data for player UUID: " + user.getUUID() + " (" + user.getName() + "). Please contact the author for help!", error);
        } finally {
            users.put(user.getUUID(), user);
        }
    }


    public Collection<User> getUsers() {
        return users.values();
    }

    public User getUser(UUID uuid) {
        return users.get(uuid);
    }

    public User getUser(OfflinePlayer player) {
        return users.get(player.getUniqueId());
    }

}
