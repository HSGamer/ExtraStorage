package me.hsgamer.extrastorage.configs.abstraction;

public interface Config {

    void set(String path, Object value);

    void save();

    void reload();

}
