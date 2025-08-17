package me.hsgamer.extrastorage.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class SoundUtil {
    public static Consumer<Player> getSoundPlayer(String soundName) {
        Consumer<Player> soundPlayer;
        if (soundName.isEmpty()) {
            soundPlayer = player -> {
            };
        } else {
            try {
                Sound parsedSound = Sound.valueOf(soundName.toUpperCase());
                soundPlayer = player -> player.playSound(player, parsedSound, 4.0f, 2.0f);
            } catch (IllegalArgumentException e) {
                soundPlayer = player -> player.playSound(player, soundName, 4.0f, 2.0f);
            }
        }
        return soundPlayer;
    }
}
