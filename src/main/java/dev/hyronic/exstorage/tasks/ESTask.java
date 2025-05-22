package dev.hyronic.exstorage.tasks;

import dev.hyronic.exstorage.ExtraStorage;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class ESTask
        extends BukkitRunnable {

    protected final ExtraStorage instance;
    @Getter
    private final boolean loop, async;

    @Setter
    protected int time;
    private int countdown;

    ESTask(ExtraStorage instance, int time, boolean loop, boolean async) {
        this.instance = instance;
        this.loop = loop;
        this.async = async;

        this.time = time;
        this.countdown = time;

        if (this.async) this.runTaskTimerAsynchronously(instance, 0L, 20L);
        else this.runTaskTimer(instance, 0L, 20L);
    }

    public void onRun() {
        // Do stuff...
    }

    public abstract void onFinish();

    @Override
    public void run() {
        this.onRun();

        if (--countdown > 0) return;
        this.countdown = time;

        this.onFinish();

        if (!loop) this.cancel();
    }

    public synchronized final void resetTime() {
        this.countdown = time;
    }

}
