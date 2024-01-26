package io.izzel.arclight.forge.mixin.bukkit;

import net.minecraft.world.level.biome.Biome;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftWorld.class, remap = false)
public class CraftWorldMixin_Forge {

    @Redirect(method = "getHumidity(III)D", at = @At(value = "FIELD", remap = true, target = "Lnet/minecraft/world/level/biome/Biome;climateSettings:Lnet/minecraft/world/level/biome/Biome$ClimateSettings;"))
    private Biome.ClimateSettings arclight$useForgeSetting(Biome instance) {
        return instance.getModifiedClimateSettings();
    }
}
