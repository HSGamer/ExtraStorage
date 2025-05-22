package dev.hyronic.exstorage.tasks;

import dev.hyronic.exstorage.ExtraStorage;
import org.bukkit.Bukkit;

public final class AutoUpdateTask
        extends ESTask {

    public AutoUpdateTask(ExtraStorage instance, int time) {
        super(instance, time, true, true);
    }

    @Override
    public void onFinish() {
        Bukkit.getServer().getOnlinePlayers().forEach(player -> instance.getUserManager().getUser(player).save());
    }

}
