package me.hsgamer.extrastorage.util;

import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class SoundUtil {
    public static Consumer<Player> getSoundPlayer(String soundName) {
        Consumer<Player> soundPlayer = player -> {
        };
        if (!soundName.isEmpty()) {
            XSound.Record soundRecord = XSound.parse(soundName);
            if (soundRecord != null) {
                if (soundRecord.getVolume() == XSound.DEFAULT_VOLUME) soundRecord.withVolume(4f);
                if (soundRecord.getPitch() == XSound.DEFAULT_PITCH) soundRecord.withPitch(2f);
                soundPlayer = player -> soundRecord.soundPlayer().forPlayers(player).play();
            }
        }
        return soundPlayer;
    }
}
