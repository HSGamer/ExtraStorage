package me.hsgamer.extrastorage.api.events;

public final class StorageLoadEvent
        extends BaseEvent {

    private boolean loaded;

    public StorageLoadEvent() {
        this.loaded = false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

}
