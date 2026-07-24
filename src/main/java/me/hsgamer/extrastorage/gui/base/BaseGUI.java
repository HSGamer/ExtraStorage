package me.hsgamer.extrastorage.gui.base;

import io.github.projectunified.craftconfig.bukkit.BukkitConfig;
import io.github.projectunified.craftconfig.proxy.ConfigGenerator;
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
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.SoundUtil;
import me.hsgamer.extrastorage.util.Utils;
import me.hsgamer.hscore.common.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseGUI<S extends Enum<S>, C extends GuiConfig, D> {
    protected final Class<S> sortClass;
    protected final Map<UUID, SpigotInventoryUI> openInventories = new HashMap<>();
    protected final Map<UUID, D> sessions = new HashMap<>();
    protected final C config;
    protected HybridMask mask;

    protected BaseGUI(String configFile, Class<C> configClass, Class<S> sortClass) {
        this.sortClass = sortClass;
        this.config = ConfigGenerator.newInstance(configClass, new BukkitConfig(ExtraStorage.getInstance(), configFile));
        loadMask();
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

    protected static <S extends Enum<S>> S getDefaultSort(GuiConfig.SettingsConfig settings, Class<S> sortClass) {
        String sort = settings.defaultSort();
        if (sort == null) return null;
        try {
            return Enum.valueOf(sortClass, sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected static String applyStoragePlaceholders(Storage storage, String playerName, String text) {
        String UNKNOWN = Utils.formatMessage(ExtraStorage.getInstance().getMessage().status().unknown());
        long space = storage.getSpace(), used = storage.getUsedSpace(), free = storage.getFreeSpace();
        double usedPercent = storage.getSpaceAsPercent(true), freePercent = storage.getSpaceAsPercent(false);
        return text
                .replaceAll(Utils.getRegex("player"), playerName)
                .replaceAll(Utils.getRegex("status"), Utils.formatMessage(storage.getStatus() ? ExtraStorage.getInstance().getMessage().status().enabled() : ExtraStorage.getInstance().getMessage().status().disabled()))
                .replaceAll(Utils.getRegex("space"), (space == -1) ? UNKNOWN : Digital.formatThousands(space))
                .replaceAll(Utils.getRegex("used(\\_|\\-)space"), (used == -1) ? UNKNOWN : Digital.formatThousands(used))
                .replaceAll(Utils.getRegex("free(\\_|\\-)space"), (free == -1) ? UNKNOWN : Digital.formatThousands(free))
                .replaceAll(Utils.getRegex("used(\\_|\\-)percent"), (usedPercent == -1) ? UNKNOWN : (usedPercent + "%"))
                .replaceAll(Utils.getRegex("free(\\_|\\-)percent"), (freePercent == -1) ? UNKNOWN : (freePercent + "%"));
    }

    public void reload() {
        openInventories.forEach((uuid, spigotInventoryUI) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            if (player.getOpenInventory().getTopInventory().getHolder() == spigotInventoryUI) {
                player.closeInventory();
            }
        });
        sessions.clear();
        openInventories.clear();
        config.reloadConfig();
        loadMask();
    }

    private void loadMask() {
        this.mask = new HybridMask();
        buildMask();
    }

    protected abstract void buildMask();

    protected SpigotInventoryUI createInventory(Player player) {
        SpigotInventoryUI inv = new SpigotInventoryUI(
                player.getUniqueId(),
                Utils.colorize(config.settings().title()),
                Digital.getBetween(9, 54, config.settings().rows() * 9)
        ) {
            @Override
            protected boolean onClick(InventoryClickEvent event) {
                SoundUtil.getSoundPlayer(config.settings().sound()).accept(player);
                if (event.getClickedInventory() == player.getOpenInventory().getBottomInventory()) {
                    onBottomInventoryClick(event);
                }
                return super.onClick(event);
            }
        };
        inv.setMask(mask);
        return inv;
    }

    public SpigotInventoryUI getInventory(Player player) {
        UUID uuid = player.getUniqueId();
        SpigotInventoryUI inv = openInventories.get(uuid);
        if (inv == null) {
            inv = createInventory(player);
            openInventories.put(uuid, inv);
        }
        return inv;
    }

    protected void onBottomInventoryClick(InventoryClickEvent event) {
    }

    protected void updateInventory(UUID uuid) {
        SpigotInventoryUI inv = openInventories.get(uuid);
        if (inv != null) inv.update();
    }

    protected void processDecorateItems() {
        Map<String, Map<String, Object>> items = config.decorateItems();
        if (items != null) {
            for (Map<String, Object> cfg : items.values()) {
                processDecorateItem(mask, cfg);
            }
        }
    }

    protected ButtonPaginatedMask createRepresentItemsMask(Function<UUID, List<Button>> buttonSupplier) {
        Map<String, Object> representConfig = config.representItem();
        List<Position> slots = getSlots(representConfig);
        ButtonPaginatedMask repMask = new ButtonPaginatedMask(u -> slots) {
            @Override
            public @NotNull List<Button> getButtons(@NotNull UUID uuid) {
                return buttonSupplier.apply(uuid);
            }
        };
        mask.add(repMask);
        return repMask;
    }

    protected void addPageNav(ButtonPaginatedMask repMask) {
        Map<String, Object> nextPageCfg = config.controlItems().nextPage();
        Map<String, Object> prevPageCfg = config.controlItems().previousPage();
        addPageNavMask(mask, repMask,
                GUIItem.get(nextPageCfg, null), getSlots(nextPageCfg),
                GUIItem.get(prevPageCfg, null), getSlots(prevPageCfg),
                this::updateInventory);
    }

    protected Stream<Item> sortRepresentItems(Stream<Item> stream, S sort, S unfilterSentinel, Function<S, Comparator<Item>> comparatorFactory) {
        if (unfilterSentinel != null && sort == unfilterSentinel) {
            return stream.filter(item -> !item.isFiltered());
        }
        stream = stream.filter(item -> item.isFiltered() || item.getQuantity() > 0);
        Comparator<Item> comparator = comparatorFactory.apply(sort);
        if (comparator != null) stream = stream.sorted(comparator);
        return stream;
    }

    protected <T extends Enum<T>> void putSortConfig(Map<T, SortButtonConfig<T>> map, T type, Map<String, Object> itemConfig) {
        if (itemConfig == null) return;
        map.put(type, new SortButtonConfig<>(GUIItem.get(itemConfig, null), getSlots(itemConfig)));
    }

    @SuppressWarnings("unchecked")
    protected <T extends Enum<T>> void addSortControls(
            Consumer<Map<T, SortButtonConfig<T>>> configurator,
            Function<UUID, T> sortAccessor,
            BiConsumer<UUID, T> sortMutator,
            Function<UUID, Boolean> orderAccessor,
            BiConsumer<UUID, Boolean> orderMutator
    ) {
        Map<T, SortButtonConfig<T>> sortMap = new EnumMap<>((Class<T>) sortClass);
        configurator.accept(sortMap);
        addSortMask(mask, sortMap, sortAccessor, sortMutator, orderAccessor, orderMutator, this::updateInventory);
    }

    protected void processDecorateItem(HybridMask mask, Map<String, Object> itemConfig) {
        List<Position> slots = getSlots(itemConfig);
        if (slots.isEmpty()) return;

        GUIItem guiItem = GUIItem.get(itemConfig, null);
        Object commands = itemConfig.get("Commands");
        List<String> actions = CollectionUtils.createStringListFromObject(commands);
        Consumer<UUID> actionConsumer = ExtraStorage.getInstance().getActionManager().createRunnable(actions);

        SimpleButtonMask decorateMask = new SimpleButtonMask();
        decorateMask.setButton(slots, (uuid, actionItem) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) return false;
            User u = ExtraStorage.getInstance().getUserManager().getUser(p);
            ItemStack item = guiItem.getItem(u, s -> s);
            if (item == null || item.getType() == Material.AIR) return false;
            actionItem.setItem(item);
            if (actionConsumer != null) {
                actionItem.setAction(InventoryClickEvent.class, event -> actionConsumer.accept(uuid));
            }
            return true;
        });
        mask.add(decorateMask);
    }

    protected void addAboutButton(HybridMask mask, Map<String, Object> aboutConfig,
                                  BiFunction<UUID, String, String> loreReplacer,
                                  BiConsumer<UUID, InventoryClickEvent> action) {
        GUIItem aboutItem = GUIItem.get(aboutConfig, null);
        List<Position> slots = getSlots(aboutConfig);
        SimpleButtonMask btnMask = new SimpleButtonMask();
        btnMask.setButton(slots, (uuid, actionItem) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) return false;
            User u = ExtraStorage.getInstance().getUserManager().getUser(p);
            aboutItem.apply(actionItem, u, s -> loreReplacer.apply(uuid, s));
            if (action != null) {
                actionItem.setAction(InventoryClickEvent.class, event -> action.accept(uuid, event));
            }
            return true;
        });
        mask.add(btnMask);
    }

    protected void addSwitchButton(HybridMask mask, Map<String, Object> switchConfig,
                                   BiConsumer<UUID, InventoryClickEvent> action) {
        GUIItem switchItem = GUIItem.get(switchConfig, null);
        List<Position> slots = getSlots(switchConfig);
        SimpleButtonMask switchMask = new SimpleButtonMask();
        mask.add(switchMask);
        switchMask.setButton(slots, (uuid, actionItem) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) return false;
            User u = ExtraStorage.getInstance().getUserManager().getUser(p);
            switchItem.apply(actionItem, u, s -> s);
            if (action != null) {
                actionItem.setAction(InventoryClickEvent.class, event -> action.accept(uuid, event));
            }
            return true;
        });
    }

    protected <T extends Enum<T>> void addSortMask(HybridMask mask, Map<T, SortButtonConfig<T>> configMap,
                                                   Function<UUID, T> sortAccessor,
                                                   BiConsumer<UUID, T> sortMutator,
                                                   Function<UUID, Boolean> orderAccessor,
                                                   BiConsumer<UUID, Boolean> orderMutator,
                                                   Consumer<UUID> updater) {
        mask.add(new Mask() {
            @Override
            public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                T sort = sortAccessor.apply(uuid);
                if (sort == null || (!configMap.containsKey(sort) && !configMap.isEmpty())) {
                    sort = configMap.keySet().stream().findFirst().orElse(null);
                    if (sort != null) sortMutator.accept(uuid, sort);
                }
                Map<Position, Consumer<ActionItem>> map = new HashMap<>();
                if (sort == null) return map;
                SortButtonConfig<T> cfg = configMap.get(sort);
                if (cfg == null || cfg.displayItem == null || cfg.positions.isEmpty()) return map;

                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return map;
                User u = ExtraStorage.getInstance().getUserManager().getUser(p);
                Consumer<ActionItem> itemConsumer = actionItem -> {
                    cfg.displayItem.apply(actionItem, u, s -> s);
                    actionItem.setAction(InventoryClickEvent.class, event -> {
                        T currentSort = sortAccessor.apply(uuid);
                        if (event.isShiftClick()) {
                            orderMutator.accept(uuid, !orderAccessor.apply(uuid));
                        } else {
                            List<T> keys = new ArrayList<>(configMap.keySet());
                            int index = keys.indexOf(currentSort);
                            T newSort;
                            if (event.isLeftClick()) {
                                newSort = keys.get((index + 1) % keys.size());
                            } else if (event.isRightClick()) {
                                newSort = keys.get((index - 1 + keys.size()) % keys.size());
                            } else {
                                newSort = null;
                            }
                            if (newSort == null) return;
                            sortMutator.accept(uuid, newSort);
                        }
                        updater.accept(uuid);
                    });
                };
                cfg.positions.forEach(position -> map.put(position, itemConsumer));
                return map;
            }
        });
    }

    protected void addPageNavMask(HybridMask mask, ButtonPaginatedMask repMask,
                                  GUIItem nextPageItem, List<Position> nextPageSlots,
                                  GUIItem previousPageItem, List<Position> previousPageSlots,
                                  Consumer<UUID> updater) {
        mask.add(new Mask() {
            @Override
            public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                Map<Position, Consumer<ActionItem>> map = new HashMap<>();
                int page = repMask.getPage(uuid);
                int maxPage = repMask.getPageAmount(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return map;
                User u = ExtraStorage.getInstance().getUserManager().getUser(p);
                UnaryOperator<String> replacer = s -> s
                        .replaceAll(Utils.getRegex("page(s)?"), Integer.toString(page + 1))
                        .replaceAll(Utils.getRegex("max(\\_|\\-)?page(s)?"), Integer.toString(maxPage));

                if (page < maxPage - 1) {
                    Consumer<ActionItem> nextConsumer = actionItem -> {
                        actionItem.setItem(nextPageItem.getItem(u, replacer));
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            repMask.nextPage(uuid);
                            updater.accept(uuid);
                        });
                    };
                    nextPageSlots.forEach(position -> map.put(position, nextConsumer));
                }
                if (page > 0) {
                    Consumer<ActionItem> prevConsumer = actionItem -> {
                        actionItem.setItem(previousPageItem.getItem(u, replacer));
                        actionItem.setAction(InventoryClickEvent.class, event -> {
                            repMask.previousPage(uuid);
                            updater.accept(uuid);
                        });
                    };
                    previousPageSlots.forEach(position -> map.put(position, prevConsumer));
                }
                return map;
            }
        });
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
