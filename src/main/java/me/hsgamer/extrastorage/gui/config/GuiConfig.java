package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.proxy.ConfigGenerator;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.SoundUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class GuiConfig {

    public final String title;
    public final int rows;
    public final Consumer<Player> soundPlayer;
    private final io.github.projectunified.craftconfig.bukkit.BukkitConfig bukkitConfig;

    public GuiConfig(Plugin plugin, String fileName, Class<?> configInterface) {
        this.bukkitConfig = new io.github.projectunified.craftconfig.bukkit.BukkitConfig(plugin, fileName);
        ConfigGenerator.newInstance(configInterface, this.bukkitConfig);
        YamlConfiguration config = this.bukkitConfig.getOriginal();
        this.title = config.getString("Settings.Title", "§lNo Title");
        this.rows = Digital.getBetween(9, 54, config.getInt("Settings.Rows") * 9);
        String soundName = config.getString("Settings.Sound", "unknown");
        this.soundPlayer = SoundUtil.getSoundPlayer(soundName);
    }

    public YamlConfiguration getConfig() {
        return bukkitConfig.getOriginal();
    }
}
