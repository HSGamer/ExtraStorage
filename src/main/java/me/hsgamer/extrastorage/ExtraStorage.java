package me.hsgamer.extrastorage;

import io.github.projectunified.craftcommand.bukkit.BukkitCommandManager;
import io.github.projectunified.craftconfig.proxy.ConfigGenerator;
import io.github.projectunified.craftux.spigot.SpigotInventoryUI;
import io.github.projectunified.craftux.spigot.SpigotInventoryUIListener;
import io.github.projectunified.faststats.bukkit.BukkitPlatform;
import io.github.projectunified.faststats.gson.GsonSerializer;
import io.github.projectunified.faststats.net.NetSubmitter;
import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.action.ActionManager;
import me.hsgamer.extrastorage.commands.AdminCommand;
import me.hsgamer.extrastorage.commands.PlayerCommand;
import me.hsgamer.extrastorage.configs.MessageConfig;
import me.hsgamer.extrastorage.configs.SettingConfig;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.data.worth.WorthManager;
import me.hsgamer.extrastorage.gui.*;
import me.hsgamer.extrastorage.gui.config.*;
import me.hsgamer.extrastorage.hooks.economy.EconomyProvider;
import me.hsgamer.extrastorage.hooks.placeholder.ESPlaceholder;
import me.hsgamer.extrastorage.listeners.ItemListener;
import me.hsgamer.extrastorage.listeners.PickupListener;
import me.hsgamer.extrastorage.listeners.PlayerListener;
import me.hsgamer.hscore.license.common.LicenseStatus;
import me.hsgamer.hscore.license.polymart.PolymartLicenseChecker;
import me.hsgamer.hscore.license.spigotmc.SpigotLicenseChecker;
import me.hsgamer.hscore.license.template.LicenseTemplate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ExtraStorage extends JavaPlugin {

    private static ExtraStorage instance;

    private boolean firstLoad;

    private SettingConfig setting;
    private MessageConfig message;
    private EconomyProvider economyProvider;
    private Consumer<Player> pickupSoundPlayer;

    private UserManager userManager;
    private WorthManager worthManager;

    private Log log;

    private ESPlaceholder placeholder;

    private StorageGUI storageGUI;
    private SellGUI sellGUI;
    private FilterGUI filterGUI;
    private PartnerGUI partnerGUI;
    private WhitelistGUI whitelistGUI;

    private ActionManager actionManager;
    private BukkitCommandManager commandManager;

    private org.bstats.bukkit.Metrics bstatsMetrics;
    private io.github.projectunified.faststats.core.Metrics fastStatsMetrics;

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

        bstatsMetrics = new org.bstats.bukkit.Metrics(this, 18779);
        fastStatsMetrics = io.github.projectunified.faststats.core.Metrics.builder()
                .platform(new BukkitPlatform(this))
                .serializer(new GsonSerializer())
                .submitter(new NetSubmitter("22928e7ae69f2235c34393792e676a7f"))
                .build();
        fastStatsMetrics.start();

        this.actionManager = new ActionManager(this);

        this.loadConfigs();
        this.userManager = new UserManager(this);
        this.loadGuiFile();

        this.log = new Log(this);

        this.registerCommands();
        this.registerEvents();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholder = new ESPlaceholder(this);
            if (placeholder.register())
                getLogger().info("Hooked into PlaceholderAPI");
        }

        this.checkLicense();
    }

    @Override
    public void onDisable() {
        if ((placeholder != null) && placeholder.isRegistered()) placeholder.unregister();
        if (commandManager != null) commandManager.unregisterAll();
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof SpigotInventoryUI) player.closeInventory();
        });
        if (userManager != null) {
            userManager.stop();
            userManager.save();
        }
        if (bstatsMetrics != null) {
            bstatsMetrics.shutdown();
        }
        if (fastStatsMetrics != null) {
            fastStatsMetrics.shutdown();
        }
    }

    private void checkLicense() {
        LicenseTemplate template = new LicenseTemplate(new SpigotLicenseChecker("90379"), new PolymartLicenseChecker("860", true, true));
        template.addDefaultMessage(this.getName());
        AsyncScheduler.get(this).run(() -> {
            Map.Entry<LicenseStatus, List<String>> result = template.getResult();
            result.getValue().forEach(result.getKey() == LicenseStatus.VALID ? getLogger()::info : getLogger()::warning);
        });
    }

    private void loadConfigs() {
        io.github.projectunified.craftconfig.bukkit.BukkitConfig settingConfig = new io.github.projectunified.craftconfig.bukkit.BukkitConfig(this, "config.yml");
        this.setting = ConfigGenerator.newInstance(SettingConfig.class, settingConfig);

        io.github.projectunified.craftconfig.bukkit.BukkitConfig messageConfig = new io.github.projectunified.craftconfig.bukkit.BukkitConfig(this, "messages.yml");
        this.message = ConfigGenerator.newInstance(MessageConfig.class, messageConfig);

        this.worthManager = new WorthManager();
        this.refreshCache();
    }

    public void refreshCache() {
        this.economyProvider = this.setting.resolveEconomyProvider();
        this.pickupSoundPlayer = this.setting.getPickupSoundPlayer();
    }

    public void loadGuiFile() {
        if (storageGUI == null) storageGUI = new StorageGUI();
        else storageGUI.reload();

        if (sellGUI == null) sellGUI = new SellGUI();
        else sellGUI.reload();

        if (filterGUI == null) filterGUI = new FilterGUI();
        else filterGUI.reload();

        if (partnerGUI == null) partnerGUI = new PartnerGUI();
        else partnerGUI.reload();

        if (whitelistGUI == null) whitelistGUI = new WhitelistGUI();
        else whitelistGUI.reload();
    }

    private void registerCommands() {
        this.commandManager = new BukkitCommandManager(this, (sender, e) -> {
            Debug.log("Error when executing the command", e);
            sender.sendMessage(ChatColor.RED + e.getMessage());
        });
        commandManager.register(new PlayerCommand());
        commandManager.register(new AdminCommand());
        commandManager.syncCommand();
    }

    private void registerEvents() {
        new PlayerListener(this);
        new SpigotInventoryUIListener(this).register();
        new ItemListener(this);
        new PickupListener(this);
    }

    public SettingConfig getSetting() {
        return this.setting;
    }

    public MessageConfig getMessage() {
        return this.message;
    }

    public EconomyProvider getEconomyProvider() {
        return this.economyProvider;
    }

    public Consumer<Player> getPickupSoundPlayer() {
        return this.pickupSoundPlayer;
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

    public ActionManager getActionManager() {
        return actionManager;
    }

    public StorageGUI getStorageGUI() {
        return storageGUI;
    }

    public SellGUI getSellGUI() {
        return sellGUI;
    }

    public FilterGUI getFilterGUI() {
        return filterGUI;
    }

    public PartnerGUI getPartnerGUI() {
        return partnerGUI;
    }

    public WhitelistGUI getWhitelistGUI() {
        return whitelistGUI;
    }
}
