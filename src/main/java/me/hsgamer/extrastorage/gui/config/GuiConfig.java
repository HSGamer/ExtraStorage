package me.hsgamer.extrastorage.gui.config;

import me.hsgamer.extrastorage.configs.types.BukkitConfig;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.SoundUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class GuiConfig
        extends BukkitConfig {

    protected String title;
    protected int rows;
    protected Consumer<Player> soundPlayer;

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

        String soundName = this.config.getString("Settings.Sound", "unknown");
        this.soundPlayer = SoundUtil.getSoundPlayer(soundName);
    }

}
