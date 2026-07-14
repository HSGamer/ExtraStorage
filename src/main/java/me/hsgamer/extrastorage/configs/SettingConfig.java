package me.hsgamer.extrastorage.configs;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import me.hsgamer.extrastorage.configs.converters.StringListConverter;
import me.hsgamer.extrastorage.configs.converters.StringMapConverter;
import me.hsgamer.extrastorage.hooks.economy.*;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.SoundUtil;
import me.hsgamer.extrastorage.util.Utils;
import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ConfigNode
@Comment("ExtraStorage plugin configuration")
public interface SettingConfig {

    void reload();

    // ==================== Sub-configs ====================

    @ConfigPath(value = "Database", priority = 10)
    DatabaseConfig database();

    @ConfigPath(value = "Economy", priority = 60)
    EconomyConfig economy();

    @ConfigPath(value = "Log", priority = 50)
    LogConfig log();

    @Comment(value = {"If enabled, the plugin will show some debug information in the console.", "Use this option only when you need to report a bug."})
    @ConfigPath(value = "Debug", priority = 0)
    default boolean debug() {
        return false;
    }

    @Comment("Date format for display")
    @ConfigPath(value = "DateFormat", priority = 20)
    default String dateFormat() {
        return "MM/dd/yyyy HH:mm:ss";
    }

    @Comment(value = {"Only store items in the storage when the player's inventory is full?", "* Note: This option will not work with PickupToStorage."})
    @ConfigPath(value = "OnlyStoreWhenInvFull", priority = 30)
    default boolean onlyStoreWhenInvFull() {
        return false;
    }

    // ==================== Top-level paths ====================

    @Comment(value = {"Time (in seconds) automatically updates the player data.", "* Cannot be disabled, default/minimum is 10 seconds."})
    @ConfigPath(value = "AutoUpdateTime", priority = 40)
    default int autoUpdateTime() {
        return 10;
    }

    @Comment(value = {"Limited the player's storage space:", "Set to -1 to disable this feature.", "* The player that has the permission 'exstorage.storage.unlimited' will not be limited the storage space.", "* Using: /esadmin space <amount> [player] to change the storage space.", "* Using: /esadmin addspace <amount> [player] to increase the storage space."})
    @ConfigPath(value = "MaxSpace", priority = 70)
    default long maxSpace() {
        return 100000;
    }

    @Comment(value = {"Blocking the player from mining the block if their storage is full?", "* This option only work if 'MaxSpace' is greater than 0."})
    @ConfigPath(value = "BlockedMining", priority = 75)
    default boolean blockedMining() {
        return true;
    }

    @Comment("Enabling this option will automatically store items in the player's storage after mining blocks.")
    @ConfigPath(value = "AutoStoreItem", priority = 80)
    default boolean autoStoreItem() {
        return true;
    }

    @Comment("Allow players to pick up items on the ground to their storage?")
    @ConfigPath(value = "PickupToStorage", priority = 85)
    default boolean pickupToStorage() {
        return true;
    }

    @Comment(value = {"Plays a sound when players pick item up:", "Leave it blank to disable this feature."})
    @ConfigPath(value = "PickupSound", priority = 90)
    default String pickupSound() {
        return "entity_item_pickup";
    }

    @Comment("List of worlds name that players cannot use this feature:")
    @ConfigPath(value = "BlacklistWorlds", priority = 100, converter = StringListConverter.class)
    default List<String> blacklistWorlds() {
        return Collections.singletonList("example_world");
    }

    @Comment(value = {"List of materials will not allow players to add them to their storage:", "* NOTE: Brackets [] are optional, <> are required."})
    @ConfigPath(value = "Blacklist", priority = 110, converter = StringListConverter.class)
    default List<String> blacklist() {
        return Collections.singletonList("BEDROCK");
    }

    @Comment(value = {"List of materials will be automatically added to the player's storage on their first join.", "Please make sure you use the correct name of the material based on the server version you are using.", "* You can still edit this option using in-game command '/esadmin whitelist'.", "!!! THIS OPTION ONLY APPLY ONCE TO PLAYERS JOINING THE SERVER FOR THE FIRST TIME !!!"})
    @ConfigPath(value = "Whitelist", priority = 120, converter = StringListConverter.class)
    default List<String> whitelist() {
        return Arrays.asList(
                "STONE", "COBBLESTONE", "COAL_BLOCK", "LAPIS_BLOCK", "REDSTONE_BLOCK",
                "IRON_BLOCK", "GOLD_BLOCK", "DIAMOND_BLOCK", "EMERALD_BLOCK",
                "COAL", "REDSTONE", "IRON_ORE", "GOLD_ORE", "DIAMOND", "EMERALD"
        );
    }

    @Comment(value = {"Limit the allowed items to be always in the whitelist.", "* If this option is enabled, the player will not be able to add items that are not in the whitelist."})
    @ConfigPath(value = "LimitWhitelist", priority = 130)
    default boolean limitWhitelist() {
        return false;
    }

    @Comment("Format the name for items that are in the player's storage:")
    @ConfigPath(value = "FormatName", priority = 140, converter = StringMapConverter.class)
    default Map<String, String> formatName() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("COBBLESTONE", "&7Cobblestone");
        map.put("COAL_BLOCK", "&8Coal Block");
        map.put("LAPIS_BLOCK", "&9Lapis Block");
        map.put("REDSTONE_BLOCK", "&cRedstone Block");
        map.put("IRON_BLOCK", "&fIron Block");
        map.put("GOLD_BLOCK", "&eGold Block");
        map.put("DIAMOND_BLOCK", "&bDiamond Block");
        map.put("EMERALD_BLOCK", "&aEmerald Block");
        return map;
    }

    void blacklistWorlds(List<String> value);

    void blacklist(List<String> value);

    void whitelist(List<String> value);

    void formatName(Map<String, String> value);

    default EconomyProvider resolveEconomyProvider() {
        String provider = economy().provider().toUpperCase();
        EconomyProvider hook;
        switch (provider) {
            case "SHOPGUIPLUS":
                hook = new ShopGuiPlusHook();
                break;
            case "ECONOMYSHOPGUI":
                hook = new EconomyShopGuiHook();
                break;
            case "PLAYERPOINTS":
                hook = new PlayerPointsHook();
                break;
            case "TOKENMANAGER":
                hook = new TokenManagerHook();
                break;
            case "ULTRAECONOMY":
                hook = new UltraEconomyHook();
                break;
            case "COINSENGINE":
            case "EXCELLENTECONOMY":
                hook = new ExcellentEconomyHook();
                break;
            case "VAULT":
                hook = new VaultHook();
                break;
            default:
                hook = new NoneEconomyHook();
                break;
        }
        if (!hook.isHooked()) {
            hook = new NoneEconomyHook();
        }
        return hook;
    }

    default Consumer<Player> getPickupSoundPlayer() {
        return SoundUtil.getSoundPlayer(pickupSound());
    }

    default SqlDatabaseSetting getSqlDatabaseSetting() {
        return new SqlDatabaseSetting() {
            @Override
            public String getHost() {
                return database().host();
            }

            @Override
            public String getPort() {
                return String.valueOf(database().port());
            }

            @Override
            public String getDatabase() {
                return database().database();
            }

            @Override
            public String getUsername() {
                return database().username();
            }

            @Override
            public String getPassword() {
                return database().password();
            }

            @Override
            public boolean isUseSSL() {
                return false;
            }

            @Override
            public Map<String, Object> getDriverProperties() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, Object> getClientProperties() {
                return Collections.emptyMap();
            }
        };
    }

    // ==================== Computed default methods ====================

    default List<String> getNormalizedBlacklist() {
        return blacklist().stream()
                .map(ItemUtil::normalizeMaterialKey)
                .collect(Collectors.toList());
    }

    default List<String> getNormalizedWhitelist() {
        List<String> normalizedBlacklist = getNormalizedBlacklist();
        return whitelist().stream()
                .map(ItemUtil::normalizeMaterialKey)
                .filter(key -> !normalizedBlacklist.contains(key))
                .collect(Collectors.toList());
    }

    default String getNameFormatted(Object key, boolean colorize) {
        String validKey = ItemUtil.toMaterialKey(key);
        if (validKey.equals(me.hsgamer.extrastorage.data.Constants.INVALID))
            return me.hsgamer.extrastorage.data.Constants.INVALID;

        String name = formatName().getOrDefault(validKey, "");
        if (!name.isEmpty()) return colorize ? Utils.colorize(name) : Utils.stripColor(name);

        io.github.projectunified.uniitem.api.Item item = ItemUtil.getItem(validKey);

        if (item instanceof ItemUtil.VanillaItem) {
            String formatName = Utils.formatName(validKey);
            return colorize ? Utils.colorize("&f" + formatName) : formatName;
        } else {
            org.bukkit.inventory.ItemStack itemStack = item.bukkitItem();
            if (itemStack == null) return me.hsgamer.extrastorage.data.Constants.INVALID;
            String finalName = itemStack.getItemMeta().getDisplayName();
            if (!colorize) finalName = Utils.stripColor(finalName);
            return finalName;
        }
    }

    default void addToWhitelist(String key) {
        List<String> list = new ArrayList<>(whitelist());
        list.add(key);
        whitelist(list);
    }

    default void removeFromWhitelist(String key) {
        List<String> list = new ArrayList<>(whitelist());
        list.remove(key);
        whitelist(list);
    }

    @ConfigNode
    @Comment("Database connection settings")
    interface DatabaseConfig {
        @Comment(value = {"Type of database:", "* Please select the type before official use.", "* There are two types: SQLite and MySQL."})
        @ConfigPath(value = "Type", priority = 0)
        default String type() {
            return "SQLite";
        }

        @Comment(value = {"For SQLite, it will be the file name.", "For MySQL, it will be the database name."})
        @ConfigPath(value = "Database", priority = 10)
        default String database() {
            return "database";
        }

        @ConfigPath(value = "Table", priority = 20)
        default String table() {
            return "exstorage_data";
        }

        @ConfigPath(value = "Host", priority = 30)
        default String host() {
            return "127.0.0.1";
        }

        @ConfigPath(value = "Port", priority = 40)
        default int port() {
            return 3306;
        }

        @ConfigPath(value = "Username", priority = 50)
        default String username() {
            return "root";
        }

        @ConfigPath(value = "Password", priority = 60)
        default String password() {
            return "";
        }
    }

    @ConfigNode
    @Comment("Economy provider settings")
    interface EconomyConfig {
        @Comment(value = {"The economy provider used for selling items.", "* Available: Vault, PlayerPoints, TokenManager, UltraEconomy, CoinsEngine.", "You can also use one of these marketplaces: ShopGUIPlus and EconomyShopGUI", "(free or paid version). It will override the default price listed in worth.yml,", "so you don't need to configure that file."})
        @ConfigPath(value = "Provider", priority = 0)
        default String provider() {
            return "Vault";
        }

        @Comment(value = {"When using UltraEconomy or CoinsEngine you may use a different currency.", "Leave it blank to use the default currency."})
        @ConfigPath(value = "Currency", priority = 10)
        default String currency() {
            return "";
        }
    }

    @ConfigNode
    @Comment("Logging settings")
    interface LogConfig {
        @Comment("Enable logging sales?")
        @ConfigPath(value = "Sales", priority = 0)
        default boolean sales() {
            return false;
        }

        @Comment("Enable item transfer logging?")
        @ConfigPath(value = "Transfer", priority = 10)
        default boolean transfer() {
            return false;
        }

        @Comment("Enable item withdrawal logging?")
        @ConfigPath(value = "Withdraw", priority = 20)
        default boolean withdraw() {
            return false;
        }
    }
}
