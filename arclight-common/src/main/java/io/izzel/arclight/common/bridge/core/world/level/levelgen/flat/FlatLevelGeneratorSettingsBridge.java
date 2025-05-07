package io.izzel.arclight.common.bridge.core.world.level.levelgen.flat;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public interface FlatLevelGeneratorSettingsBridge {
    void bridge$setBiomeSource(BiomeSource biomeSource);
    FlatLevelGeneratorSettings bridge$withBiomeSource(BiomeSource biomeSource);
    BiomeSource bridge$getBiomeSource();
}
