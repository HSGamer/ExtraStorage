package me.hsgamer.extrastorage.configs.types;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BukkitConfigChecker {

    private final BukkitConfig[] configs;

    public BukkitConfigChecker(BukkitConfig... configs) {
        this.configs = configs;
    }

    public void startTracking() {
        for (BukkitConfig cfg : configs) {
            InputStream stream = this.getClass().getResourceAsStream('/' + cfg.getFileName());
            if (stream == null) continue;

            InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            FileConfiguration source = YamlConfiguration.loadConfiguration(reader), target = cfg.config;

//			if(source.contains("ConfigVersion")) cfg.set("ConfigVersion", source.getString("ConfigVersion"));
            cfg.set("ConfigVersion", source.getString("ConfigVersion"));

            AtomicBoolean changed = new AtomicBoolean(false);
            source.getKeys(true).forEach(path -> {
                if (source.isConfigurationSection(path)) return;
                if (path.startsWith("FormatName")) return;
                if (!target.contains(path)) {
                    cfg.set(path, source.get(path));
                    changed.set(true);
                }
            });
            target.getKeys(true).forEach(path -> {
                if (target.isConfigurationSection(path)) return;
                if (path.startsWith("FormatName")) return;
                if (!source.contains(path)) {
                    cfg.set(path, null);
                    changed.set(true);
                }
            });

            if (changed.get()) cfg.save();
        }
    }

}
