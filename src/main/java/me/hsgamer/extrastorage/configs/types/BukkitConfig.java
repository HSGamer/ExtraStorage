package me.hsgamer.extrastorage.configs.types;

import me.hsgamer.extrastorage.configs.abstraction.AbstractConfig;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.logging.Level;

public abstract class BukkitConfig extends AbstractConfig {

    protected FileConfiguration config;

    protected BukkitConfig(String fileName) {
        super(fileName);

        this.config = YamlConfiguration.loadConfiguration(file);

        this.colorize(config);
        this.setup();
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }

    @Override
    public void save() {
        try {
            config.save(file);
        } catch (IOException error) {
            logger.log(Level.SEVERE, "Could not save the " + file.getName() + " file! See below for more details:", error);
        }
    }

    @Override
    public void reload() {
        try {
            config.load(file);
            this.colorize(config);
            this.setup();
        } catch (IOException error) {
            logger.log(Level.SEVERE, "Could not reload the " + file.getName() + " file! See below for more details:", error);
        } catch (InvalidConfigurationException error) {
            logger.log(Level.SEVERE, "Could not reload the " + file.getName() + " file! See below for more details:", error);
        }
    }

    public FileConfiguration getConfig() {
        return this.config;
    }
}
