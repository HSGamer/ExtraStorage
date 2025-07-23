package me.hsgamer.extrastorage.data.user;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.stub.StubUser;
import me.hsgamer.topper.data.simple.SimpleDataHolder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO: Implement saving and loading of user data
public final class UserManager extends SimpleDataHolder<UUID, UserImpl> {
    public UserManager(ExtraStorage instance) {

    }

    public void save(UUID uuid) {
        // TODO
    }

    public boolean isLoaded() {
        return true; // TODO: Implement actual check for loaded state
    }

    @Override
    public @NotNull UserImpl getDefaultValue() {
        return UserImpl.EMPTY;
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
