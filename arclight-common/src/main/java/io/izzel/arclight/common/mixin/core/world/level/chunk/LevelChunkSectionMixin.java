package io.izzel.arclight.common.mixin.core.world.level.chunk;

import io.izzel.arclight.common.bridge.core.world.chunk.LevelChunkSectionBridge;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelChunkSection.class)
public class LevelChunkSectionMixin implements LevelChunkSectionBridge {

    @Shadow @Final private PalettedContainer<Biome> biomes;

    public void setBiome(int i, int j, int k, Biome biome) {
        this.biomes.set(i, j, k, biome);
    }

    @Override
    public void bridge$setBiome(int x, int y, int z, Biome biome) {
        this.setBiome(x, y, z, biome);
    }
}
