package me.hsgamer.extrastorage.util;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.hscore.bukkit.action.SoundAction;
import me.hsgamer.hscore.common.StringReplacer;
import me.hsgamer.hscore.task.BatchRunnable;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class SoundUtil {
    public static Consumer<Player> getSoundPlayer(String soundName) {
        Consumer<Player> soundPlayer;
        if (soundName.isEmpty()) {
            soundPlayer = player -> {
            };
        } else {
            SoundAction soundAction = new SoundAction(ExtraStorage.getInstance(), soundName, 4f, 2f);
            soundPlayer = player -> {
                BatchRunnable runnable = new BatchRunnable();
                runnable.getTaskPool(0).addLast(process -> soundAction.apply(player.getUniqueId(), process, StringReplacer.DUMMY));
                runnable.run();
            };
        }
        return soundPlayer;
    }
}
