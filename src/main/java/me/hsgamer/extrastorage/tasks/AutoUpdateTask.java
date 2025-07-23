package me.hsgamer.extrastorage.tasks;

import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class AutoUpdateTask extends BukkitRunnable {
    private final ExtraStorage instance;

    public AutoUpdateTask(ExtraStorage instance, int time) {
        this.instance = instance;
        long timeInTicks = time * 20L;
        this.runTaskTimerAsynchronously(instance, timeInTicks, timeInTicks);
    }

    @Override
    public void run() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.forEach(player -> instance.getUserManager().getUser(player).save());
    }
}
