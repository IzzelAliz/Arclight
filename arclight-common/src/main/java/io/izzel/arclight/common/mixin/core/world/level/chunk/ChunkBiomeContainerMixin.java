package io.izzel.arclight.common.mixin.core.world.level.chunk;

import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBiomeContainer.class)
public class ChunkBiomeContainerMixin {

    // @formatter:off
    @Shadow @Final private Biome[] biomes;
    @Shadow @Final private static int WIDTH_BITS;
    @Shadow @Final private static int HORIZONTAL_MASK;
    @Shadow @Final private int quartMinY;
    @Shadow @Final private int quartHeight;
    // @formatter:on

    public void setBiome(int i, int j, int k, Biome biome) {
        int l = i & HORIZONTAL_MASK;
        int i1 = Mth.clamp(j - this.quartMinY, 0, this.quartHeight);
        int j1 = k & HORIZONTAL_MASK;

        this.biomes[i1 << WIDTH_BITS + WIDTH_BITS | j1 << WIDTH_BITS | l] = biome;
    }
}
