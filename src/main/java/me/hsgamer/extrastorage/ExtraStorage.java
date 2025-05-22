package me.hsgamer.extrastorage;

import lombok.Getter;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.AdminCommands;
import me.hsgamer.extrastorage.commands.PlayerCommands;
import me.hsgamer.extrastorage.commands.handler.CommandHandler;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.configs.types.BukkitConfigChecker;
import me.hsgamer.extrastorage.data.item.WorthManager;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.gui.*;
import me.hsgamer.extrastorage.gui.abstraction.GuiCreator;
import me.hsgamer.extrastorage.hooks.placeholder.ESPlaceholder;
import me.hsgamer.extrastorage.listeners.InventoryListener;
import me.hsgamer.extrastorage.listeners.PlayerListener;
import me.hsgamer.extrastorage.listeners.storage.RoseStackerPickupListener;
import me.hsgamer.extrastorage.listeners.storage.UltimateStackerPickupListener;
import me.hsgamer.extrastorage.listeners.storage.VanillaPickupListener;
import me.hsgamer.extrastorage.listeners.storage.WildStackerPickupListener;
import me.hsgamer.extrastorage.plugin.HyronicPlugin;
import me.hsgamer.extrastorage.tasks.AutoUpdateTask;
import me.hsgamer.extrastorage.util.Utils;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.hscore.database.driver.mysql.MySqlDriver;
import me.hsgamer.hscore.database.driver.sqlite.SqliteFileDriver;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class ExtraStorage
        extends HyronicPlugin {

    @Getter
    private static ExtraStorage instance;

    private boolean firstLoad;

    @Getter
    private Metrics metrics;

    @Getter
    private Setting setting;
    @Getter
    private Message message;

    @Getter
    private JavaSqlClient databaseClient;
    @Getter
    private UserManager userManager;
    @Getter
    private WorthManager worthManager;

    @Getter
    private Log log;

    @Getter
    private AutoUpdateTask autoUpdateTask;

    private ESPlaceholder placeholder;

    @Override
    public void load() {
        instance = this;
        this.firstLoad = (!this.getDataFolder().exists());
    }

    @Override
    public void enable() {
        if (firstLoad) {
            logger.warning("It seems this is the first time this plugin is run on your server.");
            logger.warning("Please take a look at the 'Whitelist' option in the config.yml file before the player data is loaded.");
            logger.warning("Once the player data was loaded, you should use '/esadmin whitelist' command to apply changes to your players' filter (do not configure it manually).");
        }

        this.metrics = new Metrics(this, 18779);

        this.loadConfigs();
        this.setupDatabase();
        this.userManager = new UserManager(this);
        this.loadGuiFile();

        this.log = new Log(this);

        this.registerCommands();
        this.registerEvents();

        if (this.isHooked("PlaceholderAPI")) {
            placeholder = new ESPlaceholder(this);
            if (placeholder.register())
                logger.info("Hooked into " + plugMan.getPlugin("PlaceholderAPI").getDescription().getFullName());
        }

        this.autoUpdateTask = new AutoUpdateTask(this, setting.getAutoUpdateTime());
    }

    @Override
    public void disable() {
        if ((placeholder != null) && placeholder.isRegistered()) placeholder.unregister();
        Bukkit.getScheduler().cancelTasks(this);
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof GuiCreator) player.closeInventory();

            User user = userManager.getUser(player);
            if (user != null) user.save();
            else
                logger.severe("Failed to save data of the player " + player.getUniqueId() + " (" + player.getName() + ").");
        });
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

        if (this.isHooked("WildStacker")) new WildStackerPickupListener(this);
        else if (this.isHooked("UltimateStacker")) new UltimateStackerPickupListener(this);
        else if (this.isHooked("RoseStacker")) new RoseStackerPickupListener(this);
        else new VanillaPickupListener(this);
    }


    private void setupDatabase() {
        me.hsgamer.hscore.database.Setting databaseSetting;
        if (setting.getDBType().equals("mysql")) {
            metrics.addCustomChart(new SimplePie("database", () -> "MySQL"));
            databaseSetting = me.hsgamer.hscore.database.Setting.create(new MySqlDriver());
            databaseSetting
                    .setHost(setting.getDBHost())
                    .setPort(Integer.toString(setting.getDBPort()))
                    .setDatabaseName(setting.getDBDatabase())
                    .setUsername(setting.getDBUsername())
                    .setPassword(setting.getDBPassword());
        } else {
            metrics.addCustomChart(new SimplePie("database", () -> "SQLite"));
            databaseSetting = me.hsgamer.hscore.database.Setting.create(new SqliteFileDriver(this.getDataFolder()));
            databaseSetting.setDatabaseName(setting.getDBDatabase());
        }
        try {
            databaseClient = new JavaSqlClient(databaseSetting);
            logger.info("Established " + setting.getDBDatabase() + " connection.");
        } catch (Exception error) {
            logger.log(Level.SEVERE, "Failed to establish " + setting.getDBDatabase() + " connection! Please contact the author for help!", error);
            return;
        }

        this.executeQueryFromFile();
    }

    private void executeQueryFromFile() {
        String query;
        try (
                InputStream inStream = this.getClass().getResourceAsStream("/sql/" + setting.getDBType() + "_query.sql");
                BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inStream))
        ) {
            query = reader.lines().reduce("", (acc, line) -> acc + line + "\n");
        } catch (IOException error) {
            logger.log(Level.SEVERE, "Failed to read database setup file!", error);
            return;
        }
        query = query.replaceAll(Utils.getRegex("table"), setting.getDBTable());

        try (Connection conn = databaseClient.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException error) {
            logger.log(Level.SEVERE, "Failed to execute query from file!", error);
            return;
        }

        logger.info("Database setup completed!");
    }
}
