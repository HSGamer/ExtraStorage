package me.hsgamer.extrastorage.builder;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.extrastorage.util.Utils;
import me.hsgamer.hscore.bukkit.utils.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemBuilder {

    private final ItemStack item;

    ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public static final class Builder {

        private final ExtraStorage instance;
        private final Pattern HDB_PATTERN;
        private String model;
        private User user;
        private Material material;
        private int amount;
        private short data;
        private String texture;
        private List<String> enchantments, hideFlags;
        private Consumer<ItemMeta> meta;

        public Builder(ItemStack item) {
            this.instance = ExtraStorage.getInstance();
            this.material = item.getType();
            this.amount = item.getAmount();
            this.data = item.getData().getData();
            this.meta = (meta) -> {
            };
            this.HDB_PATTERN = Pattern.compile("(?ium)(hdb)-(?<value>[a-zA-Z0-9]+)");
        }

        public Builder() {
            this.instance = ExtraStorage.getInstance();
            this.amount = 1;
            this.data = 0;
            this.meta = (meta) -> {
            };
            this.HDB_PATTERN = Pattern.compile("(?ium)(hdb)-(?<value>[a-zA-Z0-9]+)");
        }

        public Builder setModel(String model) {
            this.model = model;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setMaterial(Material material) {
            this.material = material;
            return this;
        }

        public Builder setAmount(int amount) {
            this.amount = Digital.getBetween(1, Integer.MAX_VALUE, amount);
            return this;
        }

        public Builder setData(short data) {
            this.data = data;
            return this;
        }

        public Builder setTexture(String texture) {
            this.texture = texture;
            return this;
        }

        public Builder setEnchantments(List<String> enchantments) {
            this.enchantments = enchantments;
            return this;
        }

        public Builder setHideFlags(List<String> hideFlags) {
            this.hideFlags = hideFlags;
            return this;
        }

        public Builder setMeta(Consumer<ItemMeta> meta) {
            this.meta = meta;
            return this;
        }

        public ItemBuilder build() {
            ItemStack item;

            if (!Strings.isNullOrEmpty(model)) {
                if (!model.contains(":")) return null;
                ItemUtil.ItemPair pair = ItemUtil.getItem(model);
                if (pair.type() == ItemUtil.ItemType.NONE) return null;
                item = pair.item();
            } else head:if (!Strings.isNullOrEmpty(texture)) {
                item = new ItemStack(Material.PLAYER_HEAD);

                Matcher matcher = HDB_PATTERN.matcher(texture);
                String encoded;
                if (matcher.find()) {
                    if (!instance.getServer().getPluginManager().isPluginEnabled("HeadDatabase")) break head;
                    String ID = matcher.group("value");
                    HeadDatabaseAPI api = new HeadDatabaseAPI();
                    encoded = api.getBase64(api.getItemHead(ID));
                } else if (texture.matches(Utils.getRegex("viewer", "player"))) encoded = user.getTexture();
                else encoded = texture;

                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (VersionUtils.isAtLeast(18)) {
                    String decoded = new String(Base64.getDecoder().decode(encoded));
                    JsonElement parse = JsonParser.parseString(decoded);
                    if (!parse.isJsonObject()) break head;

                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                    PlayerTextures textures = profile.getTextures();

                    JsonObject json = parse.getAsJsonObject();
                    String strURL = json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                    URL url;
                    try {
                        url = new URL(strURL);
                    } catch (MalformedURLException error) {
                        throw new RuntimeException("Invalid URL for textures.", error);
                    }
                    textures.setSkin(url);
                    profile.setTextures(textures);

                    meta.setOwnerProfile(profile);
                } else {
                    GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                    profile.getProperties().put("textures", new Property("textures", encoded));
                    try {
                        Field profileField = meta.getClass().getDeclaredField("profile");
                        profileField.setAccessible(true);
                        profileField.set(meta, profile);
                    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ignored) {
                    }
                }

                item.setItemMeta(meta);
            } else item = new ItemStack(material, amount, data);
            item.setAmount(amount);

            final ItemMeta meta = item.getItemMeta();
            this.meta.accept(meta);

            if ((hideFlags != null) && (!hideFlags.isEmpty())) {
                for (String list : hideFlags) {
                    try {
                        ItemFlag flag = ItemFlag.valueOf(list.toUpperCase());
                        meta.addItemFlags(flag);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            item.setItemMeta(meta);

            if ((enchantments != null) && (!enchantments.isEmpty())) {
                for (String list : enchantments) {
                    if (!list.contains(":")) continue;
                    try {
                        Enchantment enchant = Enchantment.getByName(list.split(":")[0].toUpperCase());
                        int level = Integer.parseInt(list.split(":")[1]);
                        item.addUnsafeEnchantment(enchant, level);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            return new ItemBuilder(item);
        }
    }

}
