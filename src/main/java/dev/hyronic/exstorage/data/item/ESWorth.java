package dev.hyronic.exstorage.data.item;

import dev.hyronic.exstorage.api.item.Worth;

public final class ESWorth
        implements Worth {

    private final String key;

    @Override
    public String getKey() {
        return key;
    }

    private final int quantity;

    @Override
    public int getQuantity() {
        return quantity;
    }

    private final double price;

    @Override
    public double getPrice() {
        return price;
    }

    ESWorth(String key, int quantity, double price) {
        this.key = key;
        this.quantity = quantity;
        this.price = price;
    }

}
