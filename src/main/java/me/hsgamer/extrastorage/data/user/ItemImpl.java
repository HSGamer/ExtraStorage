package me.hsgamer.extrastorage.data.user;

public class ItemImpl {
    public static final ItemImpl EMPTY = new ItemImpl(false, 0);

    public final boolean filtered;
    public final long quantity;

    private ItemImpl(boolean filtered, long quantity) {
        this.filtered = filtered;
        this.quantity = quantity;
    }

    public ItemImpl withFiltered(boolean filtered) {
        return new ItemImpl(filtered, quantity);
    }

    public ItemImpl withQuantity(long quantity) {
        return new ItemImpl(filtered, quantity);
    }
}
