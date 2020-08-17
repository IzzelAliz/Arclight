package io.izzel.arclight.common.mixin.core.world.biome;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BiomeContainer.class)
public class BiomeContainerMixin {

    // @formatter:off
    @Shadow @Final private Biome[] biomes;
    @Shadow @Final private static int WIDTH_BITS;
    // @formatter:on

    public void setBiome(int i, int j, int k, Biome biome) {
        int l = i & BiomeContainer.HORIZONTAL_MASK;
        int i2 = MathHelper.clamp(j, 0, BiomeContainer.VERTICAL_MASK);
        int j2 = k & BiomeContainer.HORIZONTAL_MASK;
        this.biomes[i2 << WIDTH_BITS + WIDTH_BITS | j2 << WIDTH_BITS | l] = biome;
    }
}
