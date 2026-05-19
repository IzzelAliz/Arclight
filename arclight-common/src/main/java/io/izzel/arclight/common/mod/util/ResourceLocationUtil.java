package io.izzel.arclight.common.mod.util;

import com.google.common.base.Preconditions;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public class ResourceLocationUtil {

    @Contract("null -> fail")
    public static String standardize(Identifier location) {
        Preconditions.checkNotNull(location, "location");
        return (location.getNamespace().equals(NamespacedKey.MINECRAFT) ? location.getPath() : location.toString())
            .replace(':', '_')
            .replaceAll("\\s+", "_")
            .replaceAll("\\W", "")
            .toUpperCase(Locale.ENGLISH);
    }

    public static String standardizeLower(Identifier location) {
        return (location.getNamespace().equals(NamespacedKey.MINECRAFT) ? location.getPath() : location.toString())
            .replace(':', '_')
            .replaceAll("\\s+", "_")
            .replaceAll("\\W", "")
            .toLowerCase(Locale.ENGLISH);
    }
}
