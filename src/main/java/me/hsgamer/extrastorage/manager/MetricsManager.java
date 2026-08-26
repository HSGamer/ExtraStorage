package me.hsgamer.extrastorage.manager;

import io.github.projectunified.faststats.bukkit.BukkitPlatform;
import io.github.projectunified.faststats.gson.GsonSerializer;
import io.github.projectunified.faststats.net.NetSubmitter;
import io.github.projectunified.minelib.plugin.base.Loadable;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class MetricsManager implements Loadable {
    private final JavaPlugin plugin;
    private Metrics bstatsMetrics;
    private io.github.projectunified.faststats.core.Metrics fastStatsMetrics;

    public MetricsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        bstatsMetrics = new Metrics(plugin, 18779);
        fastStatsMetrics = io.github.projectunified.faststats.core.Metrics.builder()
                .platform(new BukkitPlatform(plugin))
                .serializer(new GsonSerializer())
                .submitter(new NetSubmitter("22928e7ae69f2235c34393792e676a7f"))
                .build();
        fastStatsMetrics.start();
    }

    @Override
    public void disable() {
        if (bstatsMetrics != null) {
            bstatsMetrics.shutdown();
        }
        if (fastStatsMetrics != null) {
            fastStatsMetrics.shutdown();
        }
    }
}
