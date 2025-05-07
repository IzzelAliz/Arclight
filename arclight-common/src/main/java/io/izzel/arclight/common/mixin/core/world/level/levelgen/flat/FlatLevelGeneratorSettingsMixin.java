package io.izzel.arclight.common.mixin.core.world.level.levelgen.flat;

import io.izzel.arclight.common.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FlatLevelGeneratorSettings.class)
public class FlatLevelGeneratorSettingsMixin implements FlatLevelGeneratorSettingsBridge {

    @Unique
    private BiomeSource arclight$biomeSource;

    @Override
    public void bridge$setBiomeSource(BiomeSource biomeSource) {
        arclight$biomeSource = biomeSource;
    }

    @Override
    public FlatLevelGeneratorSettings bridge$withBiomeSource(BiomeSource biomeSource) {
        arclight$biomeSource = biomeSource;
        return (FlatLevelGeneratorSettings) (Object) this;
    }

    @Override
    public BiomeSource bridge$getBiomeSource() {
        return arclight$biomeSource;
    }
}
