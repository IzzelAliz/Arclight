package io.izzel.arclight.common.bridge.core.world.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.persistence.PersistentDataContainer;

public interface ChunkAccessBridge {

    PersistentDataContainer bridge$getPersistentDataContainer();

    void setBiome(int x, int y, int z, Holder<Biome> biome);

    default void setNoiseBiome(int x, int y, int z, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        this.setBiome(x, y, z, biome);
    }

}
