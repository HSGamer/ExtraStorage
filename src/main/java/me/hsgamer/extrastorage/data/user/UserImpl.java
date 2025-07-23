package me.hsgamer.extrastorage.data.user;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class UserImpl {
    public static final UserImpl EMPTY = new UserImpl(Collections.emptyMap(), "", Collections.emptyMap(), 0, false);

    public final Map<UUID, Long> partners;
    public final String texture;
    public final Map<String, ItemImpl> items;
    public final long space;
    public final boolean status;

    private UserImpl(Map<UUID, Long> partners, String texture, Map<String, ItemImpl> items, long space, boolean status) {
        this.partners = partners;
        this.texture = texture;
        this.items = items;
        this.space = space;
        this.status = status;
    }

    public UserImpl withPartners(Map<UUID, Long> partners) {
        return new UserImpl(Collections.unmodifiableMap(partners), this.texture, this.items, this.space, this.status);
    }

    public UserImpl withPartner(UUID uuid) {
        HashMap<UUID, Long> partners = new HashMap<>(this.partners);
        partners.put(uuid, System.currentTimeMillis());
        return new UserImpl(Collections.unmodifiableMap(partners), this.texture, this.items, this.space, this.status);
    }

    public UserImpl withPartnerRemoved(UUID uuid) {
        HashMap<UUID, Long> partners = new HashMap<>(this.partners);
        partners.remove(uuid);
        return new UserImpl(Collections.unmodifiableMap(partners), this.texture, this.items, this.space, this.status);
    }

    public UserImpl withTexture(String texture) {
        return new UserImpl(this.partners, texture, this.items, this.space, this.status);
    }

    public UserImpl withItems(Map<String, ItemImpl> items) {
        return new UserImpl(this.partners, this.texture, Collections.unmodifiableMap(items), this.space, this.status);
    }

    public UserImpl withItemIfNotFound(String key, ItemImpl item) {
        HashMap<String, ItemImpl> items = new HashMap<>(this.items);
        items.putIfAbsent(key, item);
        return new UserImpl(this.partners, this.texture, Collections.unmodifiableMap(items), this.space, this.status);
    }

    public UserImpl withItemRemoved(String key) {
        HashMap<String, ItemImpl> items = new HashMap<>(this.items);
        items.remove(key);
        return new UserImpl(this.partners, this.texture, Collections.unmodifiableMap(items), this.space, this.status);
    }

    public UserImpl withItemModifiedIfFound(String key, UnaryOperator<ItemImpl> modifier) {
        HashMap<String, ItemImpl> items = new HashMap<>(this.items);
        items.computeIfPresent(key, (k, v) -> modifier.apply(v));
        return new UserImpl(this.partners, this.texture, Collections.unmodifiableMap(items), this.space, this.status);
    }

    public UserImpl withSpace(long space) {
        return new UserImpl(this.partners, this.texture, this.items, space, this.status);
    }

    public UserImpl withStatus(boolean status) {
        return new UserImpl(this.partners, this.texture, this.items, this.space, status);
    }
}
