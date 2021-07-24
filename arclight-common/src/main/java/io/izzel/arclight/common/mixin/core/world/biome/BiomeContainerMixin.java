package io.izzel.arclight.common.mixin.core.world.biome;

import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBiomeContainer.class)
public class BiomeContainerMixin {

    // @formatter:off
    @Shadow @Final private Biome[] biomes;
    @Shadow @Final private static int WIDTH_BITS;
    // @formatter:on

    public void setBiome(int i, int j, int k, Biome biome) {
        int l = i & ChunkBiomeContainer.HORIZONTAL_MASK;
        int i2 = Mth.clamp(j, 0, ChunkBiomeContainer.VERTICAL_MASK);
        int j2 = k & ChunkBiomeContainer.HORIZONTAL_MASK;
        this.biomes[i2 << WIDTH_BITS + WIDTH_BITS | j2 << WIDTH_BITS | l] = biome;
    }
}
