package me.hsgamer.extrastorage;

import io.github.projectunified.craftconfig.bukkit.BukkitConfig;
import io.github.projectunified.craftconfig.proxy.ConfigGenerator;
import io.github.projectunified.minelib.plugin.base.BasePlugin;
import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import me.hsgamer.extrastorage.config.MessageConfig;
import me.hsgamer.extrastorage.config.SettingConfig;
import me.hsgamer.extrastorage.data.log.Log;
import me.hsgamer.extrastorage.gui.*;
import me.hsgamer.extrastorage.listener.*;
import me.hsgamer.extrastorage.manager.*;
import me.hsgamer.hscore.license.common.LicenseStatus;
import me.hsgamer.hscore.license.polymart.PolymartLicenseChecker;
import me.hsgamer.hscore.license.spigotmc.SpigotLicenseChecker;
import me.hsgamer.hscore.license.template.LicenseTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ExtraStorage extends BasePlugin {
    private static ExtraStorage instance;

    public static ExtraStorage getInstance() {
        return instance;
    }

    @Override
    public void load() {
        instance = this;
        boolean firstLoad = !getDataFolder().exists();
        if (firstLoad) {
            getLogger().warning("It seems this is the first time this plugin is run on your server.");
            getLogger().warning("Please take a look at the 'Whitelist' option in the config.yml file before the player data is loaded.");
            getLogger().warning("Once the player data was loaded, you should use '/esadmin whitelist' command to apply changes to your players' filter (do not configure it manually).");
        }
    }

    @Override
    protected List<Object> getComponents() {
        return Arrays.asList(
                // Configs
                ConfigGenerator.newInstance(SettingConfig.class, new BukkitConfig(this, "config.yml")),
                ConfigGenerator.newInstance(MessageConfig.class, new BukkitConfig(this, "messages.yml")),

                // Managers
                new WorthManager(this),
                new ActionManager(this),
                new UserManager(this),
                new Log(this),

                // Metrics
                new MetricsManager(this),

                // Hooks
                new HookManager(this),
                new CacheManager(this),

                // GUIs
                new StorageGUI(this),
                new SellGUI(this),
                new FilterGUI(this),
                new PartnerGUI(this),
                new WhitelistGUI(this),

                // Listeners
                new InventoryUIListener(this),
                new PlayerListener(this),
                DropListener.isAvailable() ? new DropListener(this) : new ItemListener(this),
                new PickupListener(this),

                // Commands
                new CommandManager(this)
        );
    }

    @Override
    public void enable() {
        checkLicense();
    }

    private void checkLicense() {
        LicenseTemplate template = new LicenseTemplate(new SpigotLicenseChecker("90379"), new PolymartLicenseChecker("860", true, true));
        template.addDefaultMessage(this.getName());
        AsyncScheduler.get(this).run(() -> {
            Map.Entry<LicenseStatus, List<String>> result = template.getResult();
            result.getValue().forEach(result.getKey() == LicenseStatus.VALID ? getLogger()::info : getLogger()::warning);
        });
    }
}
