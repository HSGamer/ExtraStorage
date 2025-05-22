package me.hsgamer.extrastorage.configs.abstraction;

import lombok.Getter;
import me.hsgamer.extrastorage.plugin.HyronicPlugin;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractConfig<T extends HyronicPlugin>
        implements Config {

    protected final T instance;
    protected final Logger logger;
    @Getter
    private final String fileName;
    protected File file;

    public AbstractConfig(String fileName) {
        this.instance = (T) HyronicPlugin.getInstance();
        this.logger = this.instance.getLogger();
        this.fileName = fileName;
        this.file = new File(this.instance.getDataFolder(), fileName);
        if (!file.exists()) this.instance.saveResource(fileName, true);
    }

    public abstract void setup();

    protected void colorize(ConfigurationSection config) {
        if (config == null) return;
        config.getKeys(true).forEach(path -> {
            if (config.isConfigurationSection(path)) return;
            if (config.isString(path)) config.set(path, Utils.colorize(config.getString(path)));
            else if (config.isList(path))
                config.set(path, config.getStringList(path).stream().map(Utils::colorize).collect(Collectors.toList()));
        });
    }

}
