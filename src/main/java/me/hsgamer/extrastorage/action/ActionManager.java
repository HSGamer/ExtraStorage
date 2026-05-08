package me.hsgamer.extrastorage.action;

import io.github.projectunified.maptemplate.MapTemplate;
import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.hscore.action.builder.ActionBuilder;
import me.hsgamer.hscore.action.builder.ActionInput;
import me.hsgamer.hscore.action.common.Action;
import me.hsgamer.hscore.bukkit.action.PlayerAction;
import me.hsgamer.hscore.bukkit.action.builder.BukkitActionBuilder;
import me.hsgamer.hscore.bukkit.variable.BukkitVariableBundle;
import me.hsgamer.hscore.common.StringReplacer;
import me.hsgamer.hscore.task.BatchRunnable;
import me.hsgamer.hscore.variable.VariableManager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ActionManager extends ActionBuilder<ActionInput> {
    private final ExtraStorage plugin;
    private final VariableManager variableManager;

    public ActionManager(ExtraStorage plugin) {
        this.plugin = plugin;
        BukkitActionBuilder.register(this, plugin);
        variableManager = new VariableManager();
        new BukkitVariableBundle(variableManager);
    }

    public List<Action> buildAll(List<String> list) {
        return list.stream()
                .map(s -> build(ActionInput.create(s)).orElseGet(() -> new PlayerAction(plugin, s)))
                .collect(Collectors.toList());
    }

    public UnaryOperator<String> getReplacer(UUID uuid) {
        MapTemplate mapTemplate = MapTemplate.builder()
                .setVariableFunction(s -> variableManager.tryReplace(s, uuid))
                .build();
        return s -> Objects.toString(mapTemplate.apply(s));
    }

    public Consumer<UUID> createRunnable(List<String> list) {
        if (list.isEmpty()) {
            return null;
        }
        List<Action> actions = buildAll(list);
        if (actions.isEmpty()) {
            return null;
        }
        return uuid -> {
            BatchRunnable batchRunnable = new BatchRunnable();
            StringReplacer replacer = StringReplacer.of(getReplacer(uuid));
            for (Action action : actions) {
                batchRunnable.getTaskPool(0).addLast(taskProcess -> action.apply(uuid, taskProcess, replacer));
            }
            AsyncScheduler.get(plugin).run(batchRunnable);
        };
    }
}
