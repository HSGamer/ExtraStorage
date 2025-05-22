package dev.hyronic.exstorage.gui.config;

import dev.hyronic.exstorage.configs.types.BukkitConfig;
import dev.hyronic.exstorage.util.Digital;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;

public class GuiConfig
        extends BukkitConfig {

    protected String title;
    protected int rows;
    protected Sound sound;

    public GuiConfig(String fileName) {
        super(fileName + ".yml");

        this.config = YamlConfiguration.loadConfiguration(file);

        this.colorize(config);
        this.setup();
    }

    @Override
    public void setup() {
        this.title = this.config.getString("Settings.Title", "§lNo Title");
        this.rows = Digital.getBetween(9, 54, this.config.getInt("Settings.Rows") * 9);

        String soundName = this.config.getString("Settings.Sound", "unknown").toUpperCase();
        try {
            this.sound = Sound.valueOf(soundName);
        } catch (Exception ignored) {
        }
    }

}
