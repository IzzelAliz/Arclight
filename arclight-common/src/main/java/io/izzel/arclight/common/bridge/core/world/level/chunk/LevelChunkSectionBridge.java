package io.izzel.arclight.common.bridge.core.world.level.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface LevelChunkSectionBridge {

    void bridge$setBiome(int x, int y, int z, Holder<Biome> biome);
}
