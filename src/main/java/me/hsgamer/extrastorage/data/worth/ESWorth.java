package me.hsgamer.extrastorage.data.worth;

import me.hsgamer.extrastorage.api.item.Worth;

public final class ESWorth
        implements Worth {

    private final String key;
    private final int quantity;
    private final double price;

    ESWorth(String key, int quantity, double price) {
        this.key = key;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public double getPrice() {
        return price;
    }

}
