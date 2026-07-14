package me.hsgamer.extrastorage.configs;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;

@ConfigNode
@Comment("Player-facing messages for ExtraStorage")
public interface MessageConfig {

    void reload();

    @Comment("Chat prefix for all messages")
    @ConfigPath(value = "PREFIX", priority = 0)
    default String prefix() {
        return "&8[&eᴇxᴛʀᴀ&bsᴛᴏʀᴀɢᴇ&8] &8→ &r";
    }

    // ==================== STATUS ====================

    @ConfigPath(value = "STATUS", priority = 10)
    StatusConfig status();

    @ConfigPath(value = "HELP", priority = 20)
    HelpConfig help();

    // ==================== HELP ====================

    @ConfigPath(value = "SUCCESS", priority = 30)
    SuccessConfig success();

    @ConfigPath(value = "WARN", priority = 40)
    WarnConfig warn();

    // ==================== SUCCESS ====================

    @ConfigPath(value = "FAIL", priority = 50)
    FailConfig fail();

    @ConfigNode
    interface StatusConfig {
        @ConfigPath(value = "enabled", priority = 0)
        default String enabled() {
            return "&aᴇɴᴀʙʟᴇ";
        }

        @ConfigPath(value = "disabled", priority = 10)
        default String disabled() {
            return "&cᴅɪsᴀʙʟᴇ";
        }

        @ConfigPath(value = "unknown", priority = 20)
        default String unknown() {
            return "&4ᴜɴᴋɴᴏᴡɴ";
        }

        @ConfigPath(value = "filtered", priority = 30)
        default String filtered() {
            return "&aꜰɪʟᴛᴇʀᴇᴅ";
        }

        @ConfigPath(value = "unfiltered", priority = 40)
        default String unfiltered() {
            return "&cᴜɴꜰɪʟᴛᴇʀᴇᴅ";
        }
    }

    // ==================== WARN ====================

    @ConfigNode
    interface HelpConfig {
        @ConfigPath(value = "header", priority = 0)
        default String header() {
            return "&8&l------=====[ &8[&eᴇxᴛʀᴀ&bsᴛᴏʀᴀɢᴇ&8] v{version} &8&l]=====------";
        }

        @ConfigPath(value = "footer", priority = 10)
        default String footer() {
            return "&8&l------------------------------------------";
        }

        @ConfigPath(value = "Player", priority = 20)
        PlayerHelpConfig player();

        @ConfigPath(value = "Admin", priority = 30)
        AdminHelpConfig admin();

        @ConfigNode
        interface PlayerHelpConfig {
            @ConfigPath(value = "help", priority = 0)
            default String help() {
                return "&8 > &c/{label} <help|?> &f: &7Show this help page.";
            }

            @ConfigPath(value = "open", priority = 10)
            default String open() {
                return "&8 > &c/{label} [partner] &f: &7Open your/partner''s storage.";
            }

            @ConfigPath(value = "toggle", priority = 20)
            default String toggle() {
                return "&8 > &c/{label} toggle &f: &7Change the storage usage status.";
            }

            @ConfigPath(value = "filter", priority = 30)
            default String filter() {
                return "&8 > &c/{label} filter &f: &7Open your filter.";
            }

            @ConfigPath(value = "partner", priority = 40)
            default String partner() {
                return "&8 > &c/{label} partner [add|remove|clear] [player] &f: &7Manage your partners.";
            }

            @ConfigPath(value = "sell", priority = 50)
            default String sell() {
                return "&8 > &c/{label} sell [<material-key> [amount]] &f: &7Sell your items.";
            }

            @ConfigPath(value = "withdraw", priority = 60)
            default String withdraw() {
                return "&8 > &c/{label} withdraw <material-key> [amount] &f: &7Withdraw your items.";
            }
        }

        @ConfigNode
        interface AdminHelpConfig {
            @ConfigPath(value = "help", priority = 0)
            default String help() {
                return "&8 > &c/{label} [help|?] &f: &7Show this help page.";
            }

            @ConfigPath(value = "open", priority = 10)
            default String open() {
                return "&8 > &c/{label} open <player> &f: &7Open another player''s storage.";
            }

            @ConfigPath(value = "space", priority = 20)
            default String space() {
                return "&8 > &c/{label} space <amount> [player|*] &f: &7Change the storage space.";
            }

            @ConfigPath(value = "addspace", priority = 30)
            default String addspace() {
                return "&8 > &c/{label} addspace <amount> [player|*] &f: &7Increase the storage space.";
            }

            @ConfigPath(value = "add", priority = 40)
            default String add() {
                return "&8 > &c/{label} add <material-key> <amount> [player] &f: &7Add item quantity.";
            }

            @ConfigPath(value = "subtract", priority = 50)
            default String subtract() {
                return "&8 > &c/{label} subtract <material-key> <amount> [player] &f: &7Subtract item quantity.";
            }

            @ConfigPath(value = "set", priority = 60)
            default String set() {
                return "&8 > &c/{label} set <material-key> <amount> [player] &f: &7Set item quantity.";
            }

            @ConfigPath(value = "reset", priority = 70)
            default String reset() {
                return "&8 > &c/{label} reset <material-key|*> [player] &f: &7Reset item quantity.";
            }

            @ConfigPath(value = "whitelist", priority = 80)
            default String whitelist() {
                return "&8 > &c/{label} whitelist &f: &7Edit the Whitelist option.";
            }

            @ConfigPath(value = "reload", priority = 90)
            default String reload() {
                return "&8 > &c/{label} reload &f: &7Reload the configuration.";
            }
        }
    }

    @ConfigNode
    interface SuccessConfig {
        @ConfigPath(value = "config-reload", priority = 0)
        default String configReload() {
            return "{prefix}&aSuccessfully reloaded configs!";
        }

        @ConfigPath(value = "space-changed", priority = 10)
        default String spaceChanged() {
            return "{prefix}&7The storage space has been changed to &e{space}&7.";
        }

        @ConfigPath(value = "space-increased", priority = 20)
        default String spaceIncreased() {
            return "{prefix}&7The storage space has been increased by &e{space}&7.";
        }

        @ConfigPath(value = "storage-usage-toggled", priority = 30)
        default String storageUsageToggled() {
            return "{prefix}&7You just changed the storage usage status to &r{status}&7.";
        }

        @ConfigPath(value = "filter-cleaned-up", priority = 40)
        default String filterCleanedUp() {
            return "{prefix}&7You just cleaned up your filter.";
        }

        @ConfigPath(value = "withdrew-item", priority = 50)
        default String withdrewItem() {
            return "{prefix}&aYou have withdrawn &ex{quantity} {item} &afrom the storage.";
        }

        @ConfigPath(value = "moved-items-to-storage", priority = 60)
        default String movedItemsToStorage() {
            return "{prefix}&aYou moved &ex{quantity} {item} &ato the storage.";
        }

        @ConfigPath(value = "made-partner", priority = 70)
        default String madePartner() {
            return "{prefix}&6You have just made &b{player} &6as your partner.";
        }

        @ConfigPath(value = "being-partner", priority = 80)
        default String beingPartner() {
            return "{prefix}&2You are now a partner of &b{player}&2. Using the command &e/{label} {player} &2to open their storage.";
        }

        @ConfigPath(value = "removed-partner", priority = 90)
        default String removedPartner() {
            return "{prefix}&7You have just removed &b{player} &7from your partner list.";
        }

        @ConfigPath(value = "no-longer-partner", priority = 100)
        default String noLongerPartner() {
            return "{prefix}&7You are no longer a partner of &b{player}&7.";
        }

        @ConfigPath(value = "cleanup-partners-list", priority = 110)
        default String cleanupPartnersList() {
            return "{prefix}&7You have just cleaned up your partners list.";
        }

        @ConfigPath(value = "item-sold", priority = 120)
        default String itemSold() {
            return "{prefix}&aYou sold &ex{amount} &r{item} &afor &2${price}&a.";
        }

        @ConfigPath(value = "item-added-to-whitelist", priority = 130)
        default String itemAddedToWhitelist() {
            return "{prefix}&7You added &r{item} &7to whitelist.";
        }

        @ConfigPath(value = "item-removed-from-whitelist", priority = 140)
        default String itemRemovedFromWhitelist() {
            return "{prefix}&7You removed &r{item} &7from whitelist.";
        }

        @ConfigPath(value = "Add", priority = 150)
        AddConfig add();

        @ConfigPath(value = "Subtract", priority = 160)
        SubtractConfig subtract();

        @ConfigPath(value = "Set", priority = 170)
        SetConfig set();

        @ConfigPath(value = "Reset", priority = 180)
        ResetConfig reset();

        @ConfigNode
        interface AddConfig {
            @ConfigPath(value = "self", priority = 0)
            default String self() {
                return "{prefix}&7You added &ex{quantity} {item} &7to your storage.";
            }

            @ConfigPath(value = "sender", priority = 10)
            default String sender() {
                return "{prefix}&7You added &ex{quantity} {item} &7to &b{player}''s &7storage.";
            }

            @ConfigPath(value = "target", priority = 20)
            default String target() {
                return "{prefix}&7You had &ex{quantity} {item} &7added to your storage by &b{player}&7.";
            }
        }

        @ConfigNode
        interface SubtractConfig {
            @ConfigPath(value = "self", priority = 0)
            default String self() {
                return "{prefix}&7You subtracted &ex{quantity} {item} &7from your storage.";
            }

            @ConfigPath(value = "sender", priority = 10)
            default String sender() {
                return "{prefix}&7You subtracted &ex{quantity} {item} &7from &b{player}''s &7storage.";
            }

            @ConfigPath(value = "target", priority = 20)
            default String target() {
                return "{prefix}&7You had &ex{quantity} {item} &7subtracted from your storage by &b{player}&7.";
            }
        }

        @ConfigNode
        interface SetConfig {
            @ConfigPath(value = "self", priority = 0)
            default String self() {
                return "{prefix}&7You set &r{item} &7to &ex{quantity} &7in your storage.";
            }

            @ConfigPath(value = "sender", priority = 10)
            default String sender() {
                return "{prefix}&7You set &r{item} &7to &ex{quantity} &7in &b{player}''s &7storage.";
            }

            @ConfigPath(value = "target", priority = 20)
            default String target() {
                return "{prefix}&7You had &r{item} &7set to &ex{quantity} &7in your storage by &b{player}&7.";
            }
        }

        @ConfigNode
        interface ResetConfig {
            @ConfigPath(value = "self", priority = 0)
            default String self() {
                return "{prefix}&7You reset &r{item} &7in your storage.";
            }

            @ConfigPath(value = "all", priority = 10)
            default String all() {
                return "{prefix}&7Reset quantity of all items in your storage.";
            }

            @ConfigPath(value = "all-sender", priority = 20)
            default String allSender() {
                return "{prefix}&7You reset quantity of all items in &b{player}''s &7storage.";
            }

            @ConfigPath(value = "sender", priority = 30)
            default String sender() {
                return "{prefix}&7You reset &r{item} &7in &b{player}''s &7storage.";
            }

            @ConfigPath(value = "target", priority = 40)
            default String target() {
                return "{prefix}&7You had &r{item} &7reset in your storage by &b{player}&7.";
            }
        }
    }

    // ==================== FAIL ====================

    @ConfigNode
    interface WarnConfig {
        @ConfigPath(value = "confirm-cleanup", priority = 0)
        default String confirmCleanup() {
            return "{prefix}&6Click again to confirm your cleanup!";
        }

        @ConfigPath(value = "Stored", priority = 10)
        StoredConfig stored();

        @Comment("This message will be sent when the player's storage is full: Sent using ActionBar, not using Title. Leave it blank to disable this feature.")
        @ConfigPath(value = "StorageIsFull", priority = 20)
        default String storageIsFull() {
            return "&cYour storage is full!";
        }

        @ConfigNode
        interface StoredConfig {
            @Comment("Using ActionBar to send: Leave it blank to disable this feature.")
            @ConfigPath(value = "ActionBar", priority = 0)
            default String actionBar() {
                return "&f+{amount} &r{item} &7| &e/storage &ato open storage.";
            }
        }
    }

    @ConfigNode
    interface FailConfig {
        @ConfigPath(value = "no-permission", priority = 0)
        default String noPermission() {
            return "{prefix}&cYou don''t have permission to do that!";
        }

        @ConfigPath(value = "missing-args", priority = 10)
        default String missingArgs() {
            return "{prefix}&cMissing arguments! Usage: &e{usage}&c.";
        }

        @ConfigPath(value = "only-players", priority = 20)
        default String onlyPlayers() {
            return "{prefix}&cOnly players can use this command!";
        }

        @ConfigPath(value = "only-console", priority = 30)
        default String onlyConsole() {
            return "{prefix}&cThis command can only be run by console!";
        }

        @ConfigPath(value = "not-number", priority = 40)
        default String notNumber() {
            return "{prefix}&cThe value &e{value} &cis not a valid number!";
        }

        @ConfigPath(value = "must-enter-player", priority = 50)
        default String mustEnterPlayer() {
            return "{prefix}&cPlease enter the player''s name you would like to do this!";
        }

        @ConfigPath(value = "player-not-found", priority = 60)
        default String playerNotFound() {
            return "{prefix}&cCould not find the player you have entered!";
        }

        @ConfigPath(value = "not-yourself", priority = 70)
        default String notYourself() {
            return "{prefix}&cYou cannot do that yourself!";
        }

        @ConfigPath(value = "max-space-not-used", priority = 80)
        default String maxSpaceNotUsed() {
            return "{prefix}&cYou cannot do this because the storage space is not used.";
        }

        @ConfigPath(value = "space-exceeded", priority = 90)
        default String spaceExceeded() {
            return "{prefix}&cCould not increase the storage space because the limit will be exceeded!";
        }

        @ConfigPath(value = "item-blacklisted", priority = 100)
        default String itemBlacklisted() {
            return "{prefix}&cThe blacklisted item cannot be added to the filter!";
        }

        @ConfigPath(value = "player-not-partner", priority = 110)
        default String playerNotPartner() {
            return "{prefix}&cYou are not the partner of &e{player}&c!";
        }

        @ConfigPath(value = "inventory-is-full", priority = 120)
        default String inventoryIsFull() {
            return "{prefix}&cYour inventory is full!";
        }

        @ConfigPath(value = "storage-is-full", priority = 130)
        default String storageIsFull() {
            return "{prefix}&cCould not add items to storage because it is full!";
        }

        @ConfigPath(value = "not-enough-item", priority = 140)
        default String notEnoughItem() {
            return "{prefix}&cYou (or your partner) don''t have enough &r{item} &cto do that!";
        }

        @ConfigPath(value = "not-enough-item-in-inventory", priority = 150)
        default String notEnoughItemInInventory() {
            return "{prefix}&cYou don''t have enough &r{item} &cin your inventory!";
        }

        @ConfigPath(value = "item-not-filtered", priority = 160)
        default String itemNotFiltered() {
            return "{prefix}&cYou cannot transfer items that are not in the filter to the storage!";
        }

        @ConfigPath(value = "already-partner", priority = 170)
        default String alreadyPartner() {
            return "{prefix}&cThat player is already your partner!";
        }

        @ConfigPath(value = "not-partner", priority = 180)
        default String notPartner() {
            return "{prefix}&cThat player is not your partner!";
        }

        @ConfigPath(value = "partners-list-empty", priority = 190)
        default String partnersListEmpty() {
            return "{prefix}&cYour partners list is empty!";
        }

        @ConfigPath(value = "item-not-in-storage", priority = 200)
        default String itemNotInStorage() {
            return "{prefix}&cThat item cannot be found in &e{player}''s &cstorage!";
        }

        @ConfigPath(value = "cannot-be-sold", priority = 210)
        default String cannotBeSold() {
            return "{prefix}&cYou cannot sell this item!";
        }

        @ConfigPath(value = "invalid-item", priority = 220)
        default String invalidItem() {
            return "{prefix}&cThis item is invalid!";
        }

        @ConfigPath(value = "item-already-whitelisted", priority = 230)
        default String itemAlreadyWhitelisted() {
            return "{prefix}&cThat item is already in the whitelist!";
        }
    }
}
