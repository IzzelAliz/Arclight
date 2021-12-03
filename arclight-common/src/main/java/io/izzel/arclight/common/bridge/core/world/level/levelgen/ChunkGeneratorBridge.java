package io.izzel.arclight.common.bridge.core.world.level.levelgen;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.Random;

public interface ChunkGeneratorBridge {

    void bridge$buildBedrock(ChunkAccess chunkAccess, Random random);

    WorldgenRandom bridge$buildSurface(WorldGenRegion region, ChunkAccess chunkAccess);

    void bridge$setBiomeSource(BiomeSource biomeSource);
}
