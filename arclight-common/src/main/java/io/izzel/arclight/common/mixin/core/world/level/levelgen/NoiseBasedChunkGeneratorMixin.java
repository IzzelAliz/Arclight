package io.izzel.arclight.common.mixin.core.world.level.levelgen;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Random;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin extends ChunkGeneratorMixin {

    // @formatter:off
    @Shadow public abstract void buildSurfaceAndBedrock(WorldGenRegion p_64381_, ChunkAccess p_64382_);
    @Shadow protected abstract void setBedrock(ChunkAccess pChunk, Random pRandom);
    // @formatter:on

    private transient boolean arclight$skipBedrock;
    private transient WorldgenRandom arclight$random;

    @Inject(method = "buildSurfaceAndBedrock", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;setBedrock(Lnet/minecraft/world/level/chunk/ChunkAccess;Ljava/util/Random;)V"))
    private void arclight$skipAndReturn(WorldGenRegion region, ChunkAccess chunkAccess, CallbackInfo ci, ChunkPos chunkPos, int i, int j, WorldgenRandom random) {
        if (arclight$skipBedrock) {
            arclight$random = random;
            ci.cancel();
        }
    }

    @Override
    public WorldgenRandom buildSurface(WorldGenRegion region, ChunkAccess chunkAccess) {
        try {
            arclight$skipBedrock = true;
            this.buildSurfaceAndBedrock(region, chunkAccess);
            return arclight$random;
        } finally {
            arclight$random = null;
            arclight$skipBedrock = false;
        }
    }

    @Override
    public void buildBedrock(ChunkAccess chunkAccess, Random random) {
        this.setBedrock(chunkAccess, random);
    }
}
