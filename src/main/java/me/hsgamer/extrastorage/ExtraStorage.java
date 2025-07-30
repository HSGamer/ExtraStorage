package me.hsgamer.extrastorage;

import me.hsgamer.extrastorage.commands.AdminCommands;
import me.hsgamer.extrastorage.commands.PlayerCommands;
import me.hsgamer.extrastorage.commands.handler.CommandHandler;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.configs.types.BukkitConfigChecker;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.data.worth.WorthManager;
import me.hsgamer.extrastorage.gui.*;
import me.hsgamer.extrastorage.gui.abstraction.GuiCreator;
import me.hsgamer.extrastorage.hooks.placeholder.ESPlaceholder;
import me.hsgamer.extrastorage.listeners.InventoryListener;
import me.hsgamer.extrastorage.listeners.ItemListener;
import me.hsgamer.extrastorage.listeners.PickupListener;
import me.hsgamer.extrastorage.listeners.PlayerListener;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExtraStorage extends JavaPlugin {

    private static ExtraStorage instance;

    private boolean firstLoad;

    private Metrics metrics;

    private Setting setting;
    private Message message;

    private UserManager userManager;
    private WorthManager worthManager;

    private Log log;

    private ESPlaceholder placeholder;

    public static ExtraStorage getInstance() {
        return ExtraStorage.instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        this.firstLoad = (!this.getDataFolder().exists());
    }

    @Override
    public void onEnable() {
        if (firstLoad) {
            getLogger().warning("It seems this is the first time this plugin is run on your server.");
            getLogger().warning("Please take a look at the 'Whitelist' option in the config.yml file before the player data is loaded.");
            getLogger().warning("Once the player data was loaded, you should use '/esadmin whitelist' command to apply changes to your players' filter (do not configure it manually).");
        }

        this.metrics = new Metrics(this, 18779);

        this.loadConfigs();
        this.userManager = new UserManager(this);
        this.loadGuiFile();
        this.addExtraMetrics();

        this.log = new Log(this);

        this.registerCommands();
        this.registerEvents();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholder = new ESPlaceholder(this);
            if (placeholder.register())
                getLogger().info("Hooked into PlaceholderAPI");
        }
    }

    @Override
    public void onDisable() {
        if ((placeholder != null) && placeholder.isRegistered()) placeholder.unregister();
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof GuiCreator) player.closeInventory();
        });
        if (userManager != null) {
            userManager.stop();
            userManager.save();
        }
    }


    private void loadConfigs() {
        this.setting = new Setting();
        this.message = new Message();
        this.worthManager = new WorthManager();

        new BukkitConfigChecker(setting, message).startTracking();
    }

    private void loadGuiFile() {
        new FilterGui(null, -1);
        new PartnerGui(null, -1);
        new SellGui(null, -1);
        new StorageGui(null, -1);
        new WhitelistGui(null, -1);
    }

    private void registerCommands() {
        final CommandHandler handler = new CommandHandler();
        handler.addPrimaryCommand(new AdminCommands());
        handler.addPrimaryCommand(new PlayerCommands());
    }

    private void registerEvents() {
        new PlayerListener(this);
        new InventoryListener(this);
        new ItemListener(this);
        new PickupListener(this);
    }

    private void addExtraMetrics() {
        if (instance.getSetting().getDBType().equalsIgnoreCase("mysql")) {
            metrics.addCustomChart(new SimplePie("database", () -> "MySQL"));
        } else {
            metrics.addCustomChart(new SimplePie("database", () -> "SQLite"));
        }
    }

    public Metrics getMetrics() {
        return this.metrics;
    }

    public Setting getSetting() {
        return this.setting;
    }

    public Message getMessage() {
        return this.message;
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    public WorthManager getWorthManager() {
        return this.worthManager;
    }

    public Log getLog() {
        return this.log;
    }
}
