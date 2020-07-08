package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v.util.CraftLegacy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Arrays;

@Mixin(value = CraftLegacy.class, remap = false)
public class CraftLegacyMixin {

    private static Material[] moddedMaterials;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Material[] modern_values() {
        if (moddedMaterials == null) {
            moddedMaterials = Arrays.stream(Material.values()).filter(it -> !it.isLegacy()).toArray(Material[]::new);
        }
        return moddedMaterials;
    }
}
