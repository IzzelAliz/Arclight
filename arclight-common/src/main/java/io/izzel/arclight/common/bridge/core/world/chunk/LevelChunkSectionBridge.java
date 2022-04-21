package io.izzel.arclight.common.bridge.core.world.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface LevelChunkSectionBridge {

    void bridge$setBiome(int x, int y, int z, Holder<Biome> biome);
}
