package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.legacy.CraftLegacy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = CraftLegacy.class, remap = false)
public class CraftLegacyLegacyMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Material valueOf(String name) {
        if (name.startsWith("LEGACY_")) {
            return Material.valueOf(name);
        } else {
            try {
                Material material = Material.valueOf(name);
                if (material != null && ((MaterialBridge) (Object) material).bridge$getType() == MaterialPropertySpec.MaterialType.FORGE) {
                    return material;
                } else {
                    return Material.valueOf("LEGACY_" + name);
                }
            } catch (IllegalArgumentException e) {
                return Material.valueOf("LEGACY_" + name);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Material getMaterial(String name) {
        if (name.startsWith("LEGACY_")) {
            return Material.getMaterial(name);
        } else {
            try {
                Material material = Material.getMaterial(name);
                if (material != null && ((MaterialBridge) (Object) material).bridge$getType() == MaterialPropertySpec.MaterialType.FORGE) {
                    return material;
                } else {
                    return Material.getMaterial("LEGACY_" + name);
                }
            } catch (IllegalArgumentException e) {
                return Material.getMaterial("LEGACY_" + name);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Material matchMaterial(String name) {
        if (name.startsWith("LEGACY_")) {
            return Material.matchMaterial(name);
        } else {
            try {
                Material material = Material.matchMaterial(name);
                if (((MaterialBridge) (Object) material).bridge$getType() == MaterialPropertySpec.MaterialType.FORGE) {
                    return material;
                } else {
                    return Material.matchMaterial("LEGACY_" + name);
                }
            } catch (IllegalArgumentException e) {
                return Material.matchMaterial("LEGACY_" + name);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static String name(Material material) {
        if (((MaterialBridge) (Object) material).bridge$getType() == MaterialPropertySpec.MaterialType.FORGE) {
            return material.name();
        } else {
            return material.name().substring("LEGACY_".length());
        }
    }
}
