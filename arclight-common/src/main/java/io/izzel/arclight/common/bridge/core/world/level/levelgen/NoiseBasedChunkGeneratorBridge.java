package io.izzel.arclight.common.bridge.core.world.level.levelgen;

import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.bukkit.craftbukkit.generator.CustomChunkGenerator;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface NoiseBasedChunkGeneratorBridge {

    CompletableFuture<ChunkAccess> bridge$buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState,
        StructureManager structureManager, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion,
        Set<Holder<Biome>> possibleBiomes, CustomChunkGenerator customGenerator);
}
