package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v.util.CraftLegacy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Arrays;

@Mixin(value = CraftLegacy.class, remap = false)
public class CraftLegacyUtilMixin {

    private static Material[] moddedMaterials;
    private static int offset;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Material[] modern_values() {
        if (moddedMaterials == null) {
            int origin = Material.values().length;
            moddedMaterials = Arrays.stream(Material.values()).filter(it -> !it.isLegacy()).toArray(Material[]::new);
            offset = origin - moddedMaterials.length;
        }
        return Arrays.copyOf(moddedMaterials, moddedMaterials.length);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static int modern_ordinal(Material material) {
        if (moddedMaterials == null) {
            modern_values();
        }
        if (material.isLegacy()) {
            throw new NoSuchFieldError("Legacy field ordinal: " + material);
        } else {
            int ordinal = material.ordinal();
            return ordinal < Material.LEGACY_AIR.ordinal() ? ordinal : ordinal - offset;
        }
    }
}
