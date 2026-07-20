package me.hsgamer.extrastorage.gui.base;

import io.github.projectunified.craftux.common.ActionItem;
import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.mask.HybridMask;
import io.github.projectunified.craftux.simple.SimpleButtonMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import io.github.projectunified.craftux.spigot.SpigotInventoryUtil;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.gui.util.GuiUtil;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.SoundUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public abstract class BaseGUI<S extends Enum<S>, C extends GuiConfig> extends SpigotInventoryUI {

    protected final Player player;
    protected final User user;
    protected final C config;
    private final HybridMask mask;
    private final AtomicReference<List<Button>> representItemsRef = new AtomicReference<>();
    protected Storage storage;
    protected S sort;
    protected boolean orderSort = true;

    public BaseGUI(Player player, C config, Class<S> sortClass) {
        super(player.getUniqueId(), Utils.colorize(config.settings().title()), Digital.getBetween(9, 54, config.settings().rows() * 9));
        this.player = player;
        this.user = ExtraStorage.getInstance().getUserManager().getUser(player);
        this.config = config;
        this.storage = user.getStorage();
        this.sort = getDefaultSort(config, sortClass);

        mask = new HybridMask();
        setMask(mask);
    }

    @SuppressWarnings("unchecked")
    protected static List<Position> getSlots(Map<String, Object> itemConfig) {
        Object slots = itemConfig.get("Slots");
        if (slots instanceof List) {
            return parseSlots((List<Object>) slots);
        }
        Object rawSlot = itemConfig.get("Slot");
        int slot;
        if (rawSlot instanceof Number) {
            slot = ((Number) rawSlot).intValue();
        } else {
            try {
                slot = Integer.parseInt(Objects.toString(rawSlot));
            } catch (Exception ignored) {
                return Collections.emptyList();
            }
        }
        int s = slot - 1;
        if (s >= 0 && s < 54) {
            return Collections.singletonList(SpigotInventoryUtil.toPosition(s, InventoryType.CHEST));
        }
        return Collections.emptyList();
    }

    private static List<Position> parseSlots(List<Object> slotStrs) {
        Set<Integer> slotSet = new LinkedHashSet<>();
        for (Object slot : slotStrs) {
            if (slot instanceof Number) {
                slotSet.add(((Number) slot).intValue() - 1);
            } else {
                parseSlot(Objects.toString(slot), slotSet);
            }
        }
        if (slotSet.isEmpty()) {
            return Collections.emptyList();
        }
        return slotSet.stream()
                .filter(slot -> slot >= 0 && slot < 54)
                .map(slot -> SpigotInventoryUtil.toPosition(slot, InventoryType.CHEST))
                .collect(Collectors.toList());
    }

    private static void parseSlot(String slotStr, Set<Integer> slotSet) {
        int dashIndex = slotStr.indexOf('-');
        try {
            if (dashIndex == -1) {
                slotSet.add(Integer.parseInt(slotStr.trim()) - 1);
            } else {
                int start = Integer.parseInt(slotStr.substring(0, dashIndex).trim()) - 1;
                int end = Integer.parseInt(slotStr.substring(dashIndex + 1).trim()) - 1;
                if (start > end) {
                    int temp = start;
                    start = end;
                    end = temp;
                }
                for (int i = start; i <= end; i++) {
                    slotSet.add(i);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private S getDefaultSort(C config, Class<S> sortClass) {
        String sort = config.settings().defaultSort();
        if (sort == null) return null;
        try {
            return Enum.valueOf(sortClass, sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected boolean onClick(InventoryClickEvent event) {
        SoundUtil.getSoundPlayer(config.settings().sound()).accept(player);
        return super.onClick(event);
    }

    protected final boolean hasPermission(String perm) {
        return (player.isOp() || player.hasPermission(perm));
    }

    protected void browseGUI(boolean forward) {
        GuiUtil.browseGUI(player, this, forward);
    }

    protected void setup() {
        Map<String, Map<String, Object>> decorateItems = config.decorateItems();
        if (decorateItems != null) {
            for (Map<String, Object> itemConfig : decorateItems.values()) {
                processDecorateItem(mask, itemConfig);
            }
        }

        Map<String, Object> representConfig = config.representItem();
        List<Position> representItemSlots = getSlots(representConfig);
        ButtonPaginatedMask representItemMask = new ButtonPaginatedMask(u -> representItemSlots) {
            @Override
            public @NotNull List<Button> getButtons(@NotNull UUID uuid) {
                List<Button> buttons = representItemsRef.get();
                return buttons == null ? Collections.emptyList() : buttons;
            }
        };
        mask.add(representItemMask);

        Mask controlItemMask = getControlItems(config.controlItems());
        mask.add(controlItemMask);

        GuiConfig.ControlItemsConfig controlConfig = config.controlItems();
        Map<String, Object> nextPageConfig = controlConfig.nextPage();
        GUIItem nextPageItem = GUIItem.get(nextPageConfig, null);
        List<Position> nextPageSlots = getSlots(nextPageConfig);
        Map<String, Object> previousPageConfig = controlConfig.previousPage();
        GUIItem previousPageItem = GUIItem.get(previousPageConfig, null);
        List<Position> previousPageSlots = getSlots(previousPageConfig);

        mask.add(new Mask() {
            @Override
            public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                Map<Position, Consumer<ActionItem>> map = new HashMap<>();
                int page = representItemMask.getPage(uuid);
                int maxPage = representItemMask.getPageAmount(uuid);
                UnaryOperator<String> replacer = s -> s
                        .replaceAll(Utils.getRegex("page(s)?"), Integer.toString(page + 1))
                        .replaceAll(Utils.getRegex("max(\\_|\\-)?page(s)?"), Integer.toString(maxPage));

                if (page < maxPage - 1) {
                    Consumer<ActionItem> actionItemConsumer = actionItem -> {
                        actionItem.setItem(nextPageItem.getItem(user, replacer));
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            representItemMask.nextPage(uuid);
                            update();
                        });
                    };
                    nextPageSlots.forEach(position -> map.put(position, actionItemConsumer));
                }
                if (page > 0) {
                    Consumer<ActionItem> actionItemConsumer = actionItem -> {
                        actionItem.setItem(previousPageItem.getItem(user, replacer));
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            representItemMask.previousPage(uuid);
                            update();
                        });
                    };
                    previousPageSlots.forEach(position -> map.put(position, actionItemConsumer));
                }
                return map;
            }
        });
        updateRepresentItems();
        update();
    }

    protected void updateRepresentItems() {
        representItemsRef.set(getRepresentItems(config.representItem()));
    }

    protected abstract List<Button> getRepresentItems(Map<String, Object> section);

    protected abstract Mask getControlItems(GuiConfig.ControlItemsConfig section);

    @SuppressWarnings("unchecked")
    private void processDecorateItem(HybridMask mask, Map<String, Object> itemConfig) {
        List<Position> slots = getSlots(itemConfig);
        if (slots.isEmpty()) return;

        ItemStack item = GUIItem.get(itemConfig, null).getItem(user, s -> s);
        if ((item == null) || (item.getType() == Material.AIR)) return;

        Object commands = itemConfig.get("Commands");
        List<String> actions = (commands instanceof List) ? (List<String>) commands : Collections.emptyList();
        Consumer<UUID> actionConsumer = ExtraStorage.getInstance().getActionManager().createRunnable(actions);

        SimpleButtonMask decorateButtonMask = new SimpleButtonMask();
        Button decorateButton = (uuid, actionItem) -> {
            actionItem.setItem(item);
            if (actionConsumer != null) {
                actionItem.setAction(InventoryClickEvent.class, event -> actionConsumer.accept(uuid));
            }
            return true;
        };
        decorateButtonMask.setButton(slots, decorateButton);
        mask.add(decorateButtonMask);
    }

    protected void addAboutButton(HybridMask mask, Map<String, Object> aboutConfig, UnaryOperator<String> loreReplacer, Consumer<InventoryClickEvent> action) {
        GUIItem aboutItem = GUIItem.get(aboutConfig, null);
        List<Position> aboutItemSlots = getSlots(aboutConfig);
        SimpleButtonMask aboutMask = new SimpleButtonMask();
        mask.add(aboutMask);
        aboutMask.setButton(aboutItemSlots, (uuid, actionItem) -> {
            aboutItem.apply(actionItem, user, loreReplacer);
            if (action != null) {
                actionItem.setAction(InventoryClickEvent.class, action);
            }
            return true;
        });
    }

    protected void addSwitchButton(HybridMask mask, Map<String, Object> switchConfig, Consumer<InventoryClickEvent> action) {
        GUIItem switchItem = GUIItem.get(switchConfig, null);
        List<Position> switchSlots = getSlots(switchConfig);
        SimpleButtonMask switchMask = new SimpleButtonMask();
        mask.add(switchMask);
        switchMask.setButton(switchSlots, (uuid, actionItem) -> {
            switchItem.apply(actionItem, user, s -> s);
            if (action != null) {
                actionItem.setAction(InventoryClickEvent.class, action);
            }
            return true;
        });
    }

    protected void addSortMask(HybridMask mask, Map<S, SortButtonConfig<S>> configMap) {
        if (sort == null || (!configMap.containsKey(sort) && !configMap.isEmpty())) {
            sort = configMap.keySet().stream().findFirst().orElse(null);
        }

        mask.add(new Mask() {
            @Override
            public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                Map<Position, Consumer<ActionItem>> map = new HashMap<>();
                if (sort == null) return map;
                SortButtonConfig<S> config = configMap.get(sort);
                if (config != null && config.displayItem != null && !config.positions.isEmpty()) {
                    Consumer<ActionItem> actionItemConsumer = actionItem -> {
                        config.displayItem.apply(actionItem, user, s -> s);
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            if (event.isShiftClick()) {
                                orderSort = !orderSort;
                            } else {
                                List<S> keys = new ArrayList<>(configMap.keySet());
                                int index = keys.indexOf(sort);
                                S newSort;
                                if (event.isLeftClick()) {
                                    newSort = keys.get((index + 1) % keys.size());
                                } else if (event.isRightClick()) {
                                    newSort = keys.get((index - 1 + keys.size()) % keys.size());
                                } else {
                                    newSort = null;
                                }
                                if (newSort == null) return;
                                sort = newSort;
                            }
                            updateRepresentItems();
                            update();
                        });
                    };
                    config.positions.forEach(position -> map.put(position, actionItemConsumer));
                }
                return map;
            }
        });
    }

    protected String applyStoragePlaceholders(String s, String playerName) {
        String UNKNOWN = Utils.formatMessage(ExtraStorage.getInstance().getMessage().status().unknown());
        long space = storage.getSpace(), used = storage.getUsedSpace(), free = storage.getFreeSpace();
        double usedPercent = storage.getSpaceAsPercent(true), freePercent = storage.getSpaceAsPercent(false);
        return s
                .replaceAll(Utils.getRegex("player"), playerName)
                .replaceAll(Utils.getRegex("status"), Utils.formatMessage(storage.getStatus() ? ExtraStorage.getInstance().getMessage().status().enabled() : ExtraStorage.getInstance().getMessage().status().disabled()))
                .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
    }

    protected <T extends Enum<T>> void putSortConfig(Map<T, SortButtonConfig<T>> map, T type, Map<String, Object> itemConfig) {
        if (itemConfig == null) return;
        map.put(type, new SortButtonConfig<>(GUIItem.get(itemConfig, null), getSlots(itemConfig)));
    }

    public static class SortButtonConfig<S extends Enum<S>> {
        public final GUIItem displayItem;
        public final List<Position> positions;

        public SortButtonConfig(GUIItem displayItem, List<Position> positions) {
            this.displayItem = displayItem;
            this.positions = positions;
        }
    }
}
