package me.hsgamer.extrastorage.configs;

import lombok.Getter;
import me.hsgamer.extrastorage.Debug;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.configs.types.BukkitConfig;
import me.hsgamer.extrastorage.hooks.economy.*;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public final class Setting
        extends BukkitConfig<ExtraStorage> {

    private boolean checkForUpdates;

    // Database connection:
    private String DBType, DBDatabase, DBHost, DBUsername, DBPassword, DBTable;
    private int DBPort;

    private String dateFormat;

    private boolean onlyStoreWhenInvFull;

    private int autoUpdateTime;
    private boolean restartOnChange;

    private boolean logSales, logTransfer, logWithdraw;

    private EconomyProvider economyProvider;
    private String currency;

    private long maxSpace;
    private boolean blockedMining;

    private boolean autoStoreItem, pickupToStorage;
    private Sound pickupSound;

    private List<String> blacklistWorlds, blacklist, whitelist;
    private Map<String, String> name;

    public Setting() {
        super("config.yml");
    }

    @Override
    public void setup() {
        this.checkForUpdates = config.getBoolean("CheckForUpdates");

        this.DBType = config.getString("Database.Type", "SQLite").toLowerCase();
        this.DBDatabase = config.getString("Database.Database", "database");
        this.DBHost = config.getString("Database.Host", "127.0.0.1");
        this.DBPort = config.getInt("Database.Port", 3306);
        this.DBUsername = config.getString("Database.Username", "root");
        this.DBPassword = config.getString("Database.Password", "");
        this.DBTable = config.getString("Database.Table", "exstorage_data");

        this.dateFormat = config.getString("DateFormat", "MM/dd/yyyy HH:mm:ss");

        this.onlyStoreWhenInvFull = config.getBoolean("OnlyStoreWhenInvFull");

        String economyProvider = config.getString("Economy.Provider", "VAULT").toUpperCase();
        switch (economyProvider) {
            case "SHOPGUIPLUS":
                this.economyProvider = new ShopGuiPlusHook();
                break;
            case "ECONOMYSHOPGUI":
                this.economyProvider = new EconomyShopGuiHook();
                break;
            case "PLAYERPOINTS":
                this.economyProvider = new PlayerPointsHook();
                break;
            case "TOKENMANAGER":
                this.economyProvider = new TokenManagerHook();
                break;
            case "ULTRAECONOMY":
                this.economyProvider = new UltraEconomyHook();
                break;
            case "COINSENGINE":
                this.economyProvider = new CoinsEngineHook();
                break;
            case "VAULT":
                this.economyProvider = new VaultHook();
                break;
            default:
                this.economyProvider = new NoneEconomyHook();
                break;
        }
        if (!this.economyProvider.isHooked()) {
            this.economyProvider = new NoneEconomyHook();
        }
        this.currency = config.getString("Economy.Currency", "");

        this.autoUpdateTime = Digital.getBetween(10, Integer.MAX_VALUE, config.getInt("AutoUpdateTime", 30));
        this.restartOnChange = config.getBoolean("RestartOnChange");

        this.logSales = config.getBoolean("Log.Sales");
        this.logTransfer = config.getBoolean("Log.Transfer");
        this.logWithdraw = config.getBoolean("Log.Withdraw");

        this.maxSpace = config.getLong("MaxSpace", 100000);
        this.blockedMining = config.getBoolean("BlockedMining", true);

        this.autoStoreItem = config.getBoolean("AutoStoreItem", true);
        this.pickupToStorage = config.getBoolean("PickupToStorage");
        String sound = config.getString("PickupSound", "__NO_SOUND__").toUpperCase();
        try {
            this.pickupSound = Sound.valueOf(sound);
        } catch (Exception ignored) {
            this.pickupSound = Sound.ENTITY_ITEM_PICKUP;
        }

        this.blacklistWorlds = config.getStringList("BlacklistWorlds");
        this.blacklist = config.getStringList("Blacklist")
                .stream()
                .map(string -> {
                    if (!string.contains(":")) return string.concat(":0");
                    String key = string.substring(0, string.indexOf(':'));
                    if (key.matches("(?ium)(I(tems)?A(dder)?|Oraxen)")) return string;
                    return string;
                })
                .map(ItemUtil::normalizeMaterialKey)
                .collect(Collectors.toList());
        this.whitelist = config.getStringList("Whitelist")
                .stream()
                .map(string -> {
                    if (!string.contains(":")) return string.concat(":0");
                    String key = string.substring(0, string.indexOf(':'));
                    if (key.matches("(?ium)(I(tems)?A(dder)?|Oraxen)")) return string;
                    return string;
                })
                .map(ItemUtil::normalizeMaterialKey)
                .filter(key -> (!blacklist.contains(key)))
                .collect(Collectors.toList());

        this.name = new HashMap<>();
        config.getConfigurationSection("FormatName")
                .getKeys(false)
                .forEach(key -> name.put(ItemUtil.normalizeMaterialKey(key), config.getString("FormatName." + key)));

        Debug.enabled = config.getBoolean("Debug");
    }

    public void addToWhitelist(String key) {
        whitelist.add(key);
        this.set("Whitelist", whitelist);
        this.save();
    }

    public void removeFromWhitelist(String key) {
        whitelist.remove(key);
        this.set("Whitelist", whitelist);
        this.save();
    }

    public String getNameFormatted(Object key, boolean colorize) {
        String validKey = ItemUtil.toMaterialKey(key);

        String name = this.name.getOrDefault(validKey, "");
        if (!name.isEmpty()) return (colorize ? name : Utils.stripColor(name));

        ItemUtil.ItemPair pair = ItemUtil.getItem(validKey);
        if (pair.type() != ItemUtil.ItemType.NONE && pair.type() != ItemUtil.ItemType.VANILLA) {
            String finalName = pair.item().getItemMeta().getDisplayName();
            if (!colorize) Utils.stripColor(finalName);
            return finalName;
        }

        String formatName = Utils.formatName(validKey);
        return (colorize ? Utils.colorize("&f" + formatName) : formatName);
    }

}
