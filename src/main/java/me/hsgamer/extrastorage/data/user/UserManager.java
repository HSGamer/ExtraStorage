package me.hsgamer.extrastorage.data.user;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.stub.StubUser;
import me.hsgamer.extrastorage.fetcher.TextureFetcher;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.simple.SimpleDataHolder;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.sql.converter.UUIDSqlValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlDataStorageSupplier;
import me.hsgamer.topper.storage.sql.mysql.MySqlDataStorageSupplier;
import me.hsgamer.topper.storage.sql.sqlite.SqliteDataStorageSupplier;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class UserManager extends SimpleDataHolder<UUID, UserImpl> {
    private final ExtraStorage instance;
    private final DataStorage<UUID, UserImpl> storage;
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final AtomicReference<ConcurrentHashMap<UUID, UserImpl>> saveMapRef = new AtomicReference<>(new ConcurrentHashMap<>());

    public UserManager(ExtraStorage instance) {
        this.instance = instance;
        boolean isMySql = instance.getSetting().getDBType().equalsIgnoreCase("mysql");
        SqlDataStorageSupplier.Options options = SqlDataStorageSupplier.options()
                .setIncrementalKey("id")
                .setClientFunction(JavaSqlClient::new);
        SqlDataStorageSupplier supplier = isMySql
                ? new MySqlDataStorageSupplier(instance.getSetting().getSqlDatabaseSetting(), options)
                : new SqliteDataStorageSupplier(instance.getDataFolder(), instance.getSetting().getSqlDatabaseSetting(), options);
        this.storage = supplier.getStorage(
                instance.getSetting().getDBTable(),
                new UUIDSqlValueConverter("uuid"),
                UserImpl.getConverter(isMySql)
        );

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            try {
                this.storage.onRegister();
                this.storage.load().forEach((uuid, user) -> getOrCreateEntry(uuid).setValue(user, false));
            } catch (Exception e) {
                instance.getLogger().log(Level.SEVERE, "Error while loading user", e);
            } finally {
                loaded.set(true);
            }
        });

        long autoUpdateTime = instance.getSetting().getAutoUpdateTime() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, () -> save(), autoUpdateTime, autoUpdateTime);
    }

    public void save() {
        Map<UUID, UserImpl> map = saveMapRef.get();
        if (map.isEmpty()) return;
        saveMapRef.set(new ConcurrentHashMap<>());

        Optional<DataStorage.Modifier<UUID, UserImpl>> optionalModifier = storage.modify();
        if (!optionalModifier.isPresent()) {
            instance.getLogger().log(Level.WARNING, "Failed to get modifier for user storage");
            return;
        }
        DataStorage.Modifier<UUID, UserImpl> modifier = optionalModifier.get();

        try {
            modifier.save(map);
            modifier.commit();
        } catch (Exception e) {
            instance.getLogger().log(Level.SEVERE, "Error while saving user data", e);
            modifier.rollback();
        }
    }

    public void save(UUID uuid) {
        Map<UUID, UserImpl> saveMap = saveMapRef.get();
        UserImpl toSave = saveMap.get(uuid);
        if (toSave == null) return;
        saveMap.remove(uuid);

        Optional<DataStorage.Modifier<UUID, UserImpl>> optionalModifier = storage.modify();
        if (!optionalModifier.isPresent()) {
            ExtraStorage.getInstance().getLogger().log(Level.WARNING, "Failed to get modifier for user storage");
            return;
        }
        DataStorage.Modifier<UUID, UserImpl> modifier = optionalModifier.get();
        try {
            modifier.save(Collections.singletonMap(uuid, toSave));
            modifier.commit();
        } catch (Exception e) {
            ExtraStorage.getInstance().getLogger().log(Level.SEVERE, "Error while saving user data for " + uuid, e);
            modifier.rollback();
        }
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    @Override
    public @NotNull UserImpl getDefaultValue() {
        return UserImpl.EMPTY;
    }

    @Override
    public void onCreate(DataEntry<UUID, UserImpl> entry) {
        Map<String, ItemImpl> map = instance.getSetting().getWhitelist().stream()
                .map(ItemUtil::normalizeMaterialKey)
                .filter(Objects::nonNull)
                .filter(key -> !key.trim().isEmpty())
                .distinct()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> ItemImpl.EMPTY.withFiltered(true)
                ));
        entry.setValue(user -> user.withItems(map), false);
    }

    @Override
    public void onUpdate(DataEntry<UUID, UserImpl> entry, UserImpl oldValue, UserImpl newValue) {
        saveMapRef.get().put(entry.getKey(), newValue);
    }

    public void load(UUID uuid) {
        DataEntry<UUID, UserImpl> entry = getOrCreateEntry(uuid);
        if (entry.getValue().texture.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                String name = player.getName();
                if (name == null || name.isEmpty()) return;
                String textureUrl = TextureFetcher.getTextureUrl(name);
                if (textureUrl == null || textureUrl.isEmpty()) return;
                byte[] texture = ("{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}").getBytes();
                String textureString = new String(Base64.getEncoder().encode(texture));
                entry.setValue(user -> user.withTexture(textureString));
            });
        }
    }

    public Collection<User> getUsers() {
        return getEntryMap().values().stream().map(StubUser::new).collect(Collectors.toSet());
    }

    public User getUser(UUID uuid) {
        return new StubUser(getOrCreateEntry(uuid));
    }

    public User getUser(OfflinePlayer player) {
        return getUser(player.getUniqueId());
    }
}
