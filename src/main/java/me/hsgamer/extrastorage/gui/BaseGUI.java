package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.ActionItem;
import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.common.Position;
import io.github.projectunified.craftux.mask.ButtonPaginatedMask;
import io.github.projectunified.craftux.mask.HybridMask;
import io.github.projectunified.craftux.simple.SimpleButton;
import io.github.projectunified.craftux.simple.SimpleButtonMask;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import io.github.projectunified.craftux.spigot.SpigotInventoryUtil;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.gui.config.GuiConfig;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class BaseGUI<S extends Enum<S>> extends SpigotInventoryUI {
    private static final Pattern SLOT_PATTERN = Pattern.compile("(?<start>\\d+)-(?<end>\\d+)");

    protected final Player player;
    protected final User user;
    protected final GuiConfig config;
    protected Storage storage;
    protected S sort;
    protected boolean orderSort = true;

    private final HybridMask mask;
    private final AtomicReference<List<Button>> representItemsRef = new AtomicReference<>();
    private final ConfigurationSection decorateItemSection;
    private final ConfigurationSection representItemSection;
    private final ConfigurationSection controlItemSection;

    public BaseGUI(Player player, GuiConfig config, Class<S> sortClass) {
        super(player.getUniqueId(), config.title, config.rows);
        this.player = player;
        this.user = ExtraStorage.getInstance().getUserManager().getUser(player);
        this.config = config;
        this.storage = user.getStorage();
        this.sort = getDefaultSort(config, sortClass);

        decorateItemSection = Objects.requireNonNull(config.getConfig().getConfigurationSection("DecorateItems"), "DecorateItems must not be null!");
        representItemSection = Objects.requireNonNull(config.getConfig().getConfigurationSection("RepresentItem"), "RepresentItem must not be null!");
        controlItemSection = Objects.requireNonNull(config.getConfig().getConfigurationSection("ControlItems"), "ControlItems must not be null!");

        mask = new HybridMask();
        setMask(mask);
    }

    private S getDefaultSort(GuiConfig config, Class<S> sortClass) {
        try {
            return Enum.valueOf(sortClass, config.getConfig().getString("Settings.DefaultSort", "__INVALID__").toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallbackSort();
        }
    }

    protected abstract S fallbackSort();

    @Override
    protected boolean onClick(InventoryClickEvent event) {
        config.soundPlayer.accept(player);
        return super.onClick(event);
    }

    protected final boolean hasPermission(String perm) {
        return (player.isOp() || player.hasPermission(perm));
    }

    protected static List<Position> getSlots(ConfigurationSection config) {
        if (config.contains("Slot")) {
            int slot = config.getInt("Slot");
            if ((slot > 0) && (slot < 55)) return Collections.singletonList(SpigotInventoryUtil.toPosition(slot, InventoryType.CHEST));
        }

        if (config.contains("Slots")) {
            List<String> slotList = config.getStringList("Slots");
            if (!slotList.isEmpty()) return getSlots(slotList);
        }

        return Collections.emptyList();
    }

    protected static List<Position> getSlots(List<String> slotList) {
        Set<Integer> slotSet = new LinkedHashSet<>();
        for (String slotStr : slotList) {
            Matcher matcher = SLOT_PATTERN.matcher(slotStr);
            try {
                if (!matcher.find()) slotSet.add(Digital.getBetween(1, 54, Integer.parseInt(slotStr)));
                else {
                    int start = Digital.getBetween(1, 54, Integer.parseInt(matcher.group("start")));
                    int end = Digital.getBetween(start, 54, Integer.parseInt(matcher.group("end")));
                    for (int i = start; i <= end; i++) slotSet.add(i);
                }
            } catch (NumberFormatException ignored) {
                // Ignore
            }
        }
        return slotSet.stream()
                .map(slotElement -> SpigotInventoryUtil.toPosition(slotElement, InventoryType.CHEST))
                .collect(Collectors.toList());
    }

    protected static List<Position> getSlots(String slot) {
        return getSlots(Collections.singletonList(slot));
    }

    protected void setup() {
        Mask decorateItemMask = getDecorateItems(decorateItemSection);
        mask.add(decorateItemMask);

        List<Position> representItemSlots = getSlots(representItemSection);
        ButtonPaginatedMask representItemMask = new ButtonPaginatedMask(u -> representItemSlots) {
            @Override
            public @NotNull List<Button> getButtons(@NotNull UUID uuid) {
                List<Button> buttons = representItemsRef.get();
                return buttons == null ? Collections.emptyList() : buttons;
            }
        };
        mask.add(representItemMask);

        Mask controlItemMask = getControlItems(controlItemSection);
        mask.add(controlItemMask);

        ConfigurationSection nextPageSection = Objects.requireNonNull(controlItemSection.getConfigurationSection("NextPage"), "NextPage must not be null!");
        ConfigurationSection previousPageSection = Objects.requireNonNull(controlItemSection.getConfigurationSection("PreviousPage"), "PreviousPage must not be null!");

        GUIItem nextPageItem = GUIItem.get(nextPageSection, null);
        List<Position> nextPageSlots = getSlots(nextPageSection);
        GUIItem previousPageItem = GUIItem.get(previousPageSection, null);
        List<Position> previousPageSlots = getSlots(previousPageSection);

        mask.add(new Mask() {
                @Override
                public @NotNull Map<Position, Consumer<ActionItem>> apply(@NotNull UUID uuid) {
                    Map<Position, Consumer<ActionItem>> map = new HashMap<>();
                    int page = representItemMask.getPage(uuid);
                    int maxPage = representItemMask.getPageAmount(uuid);
                    UnaryOperator<String> replacer = s -> s
                            .replaceAll(Utils.getRegex("page(s)?"), Integer.toString(page + 1))
                            .replaceAll(Utils.getRegex("max(\\_|\\-)?page(s)?"), Integer.toString(maxPage));

                    if (nextPageItem != null && page < maxPage - 1) {
                        Consumer<ActionItem> actionItemConsumer = actionItem -> {
                            actionItem.setItem(nextPageItem.getItem(user, replacer));
                            actionItem.setAction(InventoryClickEvent.class, event -> {
                                representItemMask.nextPage(uuid);
                                update();
                            });
                        };
                        nextPageSlots.forEach(position -> map.put(position, actionItemConsumer));
                    }
                    if (previousPageItem != null && page > 0) {
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
        representItemsRef.set(getRepresentItems(representItemSection));
    }

    protected abstract List<Button> getRepresentItems(ConfigurationSection section);

    protected abstract Mask getControlItems(ConfigurationSection section);

    protected Mask getDecorateItems(ConfigurationSection section) {
        HybridMask mask = new HybridMask();
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof ConfigurationSection)) continue;
            ConfigurationSection decorateItemSection = (ConfigurationSection) value;

            List<Position> slots = getSlots(decorateItemSection);
            if (slots.isEmpty()) continue;

            ItemStack item = GUIItem.get(decorateItemSection, null).getItem(user, s -> s);
            if ((item == null) || (item.getType() == Material.AIR)) continue;

            SimpleButtonMask decorateButtonMask = new SimpleButtonMask();
            SimpleButton decorateButton = new SimpleButton(item);
            decorateButtonMask.setButton(slots, decorateButton);
            mask.add(decorateButtonMask);
        }
        return mask;
    }
}
