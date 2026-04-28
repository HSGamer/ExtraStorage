package me.hsgamer.extrastorage.gui.util;

import org.bukkit.inventory.meta.ItemMeta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class CompatItemUtil {
    private static MethodHandle setCustomModelData;
    private static MethodHandle constructNamespacedKey;
    private static MethodHandle setItemModel;

    static {
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        try {
            setCustomModelData = publicLookup.findVirtual(ItemMeta.class, "setCustomModelData", MethodType.methodType(void.class, Integer.class));
        } catch (Throwable e) {
            setCustomModelData = null;
        }

        try {
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey");
            constructNamespacedKey = publicLookup.findStatic(namespacedKey, "fromString", MethodType.methodType(namespacedKey, String.class));
            setItemModel = publicLookup.findVirtual(ItemMeta.class, "setItemModel", MethodType.methodType(void.class, namespacedKey));
        } catch (Throwable e) {
            constructNamespacedKey = null;
            setItemModel = null;
        }
    }

    private CompatItemUtil() {

    }

    public static void setCustomModelData(ItemMeta meta, int data) {
        if (setCustomModelData == null) return;

        try {
            setCustomModelData.invoke(meta, data);
        } catch (Throwable ignored) {
            // IGNORED
        }
    }

    public static void setItemModel(ItemMeta meta, String model) {
        if (constructNamespacedKey == null || setItemModel == null) return;

        try {
            Object namespacedKey = constructNamespacedKey.invoke(model);
            setItemModel.invoke(meta, namespacedKey);
        } catch (Throwable ignored) {
            // IGNORED
        }
    }
}
