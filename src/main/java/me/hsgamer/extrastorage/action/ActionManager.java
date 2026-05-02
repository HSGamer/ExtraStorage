package me.hsgamer.extrastorage.action;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.hscore.action.builder.ActionBuilder;
import me.hsgamer.hscore.action.builder.ActionInput;
import me.hsgamer.hscore.action.common.Action;
import me.hsgamer.hscore.bukkit.action.PlayerAction;
import me.hsgamer.hscore.bukkit.action.builder.BukkitActionBuilder;
import me.hsgamer.hscore.bukkit.variable.BukkitVariableBundle;
import me.hsgamer.hscore.task.BatchRunnable;
import me.hsgamer.hscore.variable.VariableManager;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
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
        List<ActionInput> actionInputs = list.stream().map(ActionInput::create).collect(Collectors.toList());
        return build(actionInputs, actionInput -> new PlayerAction(plugin, actionInput.getValue()));
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
            for (Action action : actions) {
                batchRunnable.getTaskPool(0).addLast(taskProcess -> action.apply(uuid, taskProcess, variableManager));
            }
            AsyncScheduler.get(plugin).run(batchRunnable);
        };
    }
}
