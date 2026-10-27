package io.izzel.arclight.common.bridge.core.world.level.chunk;

import net.minecraft.world.level.biome.BiomeSource;

public interface ChunkGeneratorBridge {

    void bridge$setBiomeSource(BiomeSource biomeSource);

    void bridge$applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.world.level.StructureManager structureManager, boolean vanilla);
}
