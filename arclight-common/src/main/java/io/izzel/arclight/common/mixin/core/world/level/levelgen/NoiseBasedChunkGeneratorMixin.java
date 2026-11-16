package io.izzel.arclight.common.mixin.core.world.level.levelgen;

import io.izzel.arclight.common.bridge.core.world.level.LevelAccessorBridge;
import io.izzel.arclight.common.bridge.core.world.level.levelgen.NoiseBasedChunkGeneratorBridge;
import io.izzel.arclight.common.bridge.core.world.level.levelgen.WorldGenerationContextBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import org.bukkit.craftbukkit.generator.CustomChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin implements NoiseBasedChunkGeneratorBridge {

    @Unique
    private final ThreadLocal<CustomChunkGenerator> arclight$customGenerator = new ThreadLocal<>();

    @Unique
    private final ThreadLocal<Level> arclight$generationLevel = new ThreadLocal<>();

    @Unique
    private void arclight$restoreGenerationLevel(Level previous) {
        if (previous == null) {
            this.arclight$generationLevel.remove();
        } else {
            this.arclight$generationLevel.set(previous);
        }
    }

    @Decorate(method = "buildSurface", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/minecraft/world/level/levelgen/WorldGenerationContext;"), require = 1, allow = 1)
    private WorldGenerationContext arclight$surfaceContext(ChunkGenerator generator, LevelHeightAccessor height) throws Throwable {
        WorldGenerationContext context = (WorldGenerationContext) DecorationOps.callsite().invoke(generator, height);
        ((WorldGenerationContextBridge) (Object) context).bridge$setLevel(this.arclight$generationLevel.get());
        return context;
    }

    @Decorate(method = "generateCarvers", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/LevelHeightAccessor;)Lnet/minecraft/world/level/levelgen/WorldGenerationContext;"), require = 1, allow = 1)
    private WorldGenerationContext arclight$carverContext(ChunkGenerator generator, LevelHeightAccessor height,
            @Local(ordinal = 0) WorldGenRegion region) throws Throwable {
        WorldGenerationContext context = (WorldGenerationContext) DecorationOps.callsite().invoke(generator, height);
        ((WorldGenerationContextBridge) (Object) context).bridge$setLevel(region == null ? this.arclight$generationLevel.get() : region.getLevel());
        return context;
    }

    @Override
    public CompletableFuture<ChunkAccess> bridge$buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState,
            StructureManager structureManager, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion,
            Set<Holder<Biome>> possibleBiomes, CustomChunkGenerator customGenerator) {
        CustomChunkGenerator previous = this.arclight$customGenerator.get();
        this.arclight$customGenerator.set(customGenerator);
        try {
            return ((NoiseBasedChunkGenerator) (Object) this).buildTerrain(chunk, blender, randomState,
                structureManager, biomeManager, carverBiomeRegion, possibleBiomes);
        } finally {
            this.arclight$restoreGenerator(previous);
        }
    }

    @Unique
    private void arclight$restoreGenerator(CustomChunkGenerator previous) {
        if (previous == null) {
            this.arclight$customGenerator.remove();
        } else {
            this.arclight$customGenerator.set(previous);
        }
    }

    @Decorate(method = "buildTerrain", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"), require = 1, allow = 1)
    private CompletableFuture<ChunkAccess> arclight$carryGenerator(Supplier<ChunkAccess> work, Executor executor) throws Throwable {
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        Supplier<ChunkAccess> wrapped = work;
        if (generator != null) {
            wrapped = () -> {
                CustomChunkGenerator previous = this.arclight$customGenerator.get();
                this.arclight$customGenerator.set(generator);
                try {
                    return work.get();
                } finally {
                    this.arclight$restoreGenerator(previous);
                }
            };
        }
        return (CompletableFuture<ChunkAccess>) DecorationOps.callsite().invoke(wrapped, executor);
    }

    @Decorate(method = "buildTerrain", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;completedFuture(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;"), require = 1, allow = 1)
    private CompletableFuture<ChunkAccess> arclight$emptyTerrain(Object value) throws Throwable {
        CompletableFuture<ChunkAccess> original = (CompletableFuture<ChunkAccess>) DecorationOps.callsite().invoke(value);
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        if (generator == null) {
            return original;
        }
        return original.thenApplyAsync(chunk -> {
            generator.fillFromNoise(chunk);
            generator.buildSurface(chunk);
            generator.applyCarvers(chunk);
            return chunk;
        }, Util.backgroundExecutor());
    }

    @Decorate(method = "lambda$buildTerrain$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;doFill(Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"), require = 1, allow = 1)
    private void arclight$shouldGenerateNoise(NoiseBasedChunkGenerator self, NoiseChunk noise, ChunkAccess chunk) throws Throwable {
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        if (generator == null || generator.shouldGenerateNoise(chunk)) {
            DecorationOps.callsite().invoke(self, noise, chunk);
        }
    }

    @Decorate(method = "lambda$buildTerrain$0", inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;settings:Lnet/minecraft/core/Holder;"), require = 1, allow = 1)
    private void arclight$generateNoise(@Local(ordinal = 0) ChunkAccess chunk) {
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        if (generator != null) {
            generator.fillFromNoise(chunk);
        }
    }

    @Decorate(method = "lambda$buildTerrain$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Ljava/util/Set;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V"), require = 1, allow = 1)
    private void arclight$shouldGenerateSurface(NoiseBasedChunkGenerator self, ChunkAccess chunk, NoiseChunk noise,
            RandomState random, BiomeManager biomes, Set<Holder<Biome>> possibleBiomes, MaterialRule rule,
            @Local(ordinal = 0) StructureManager structureManager) throws Throwable {
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        if (generator == null || generator.shouldGenerateSurface(chunk)) {
            Level previous = this.arclight$generationLevel.get();
            this.arclight$generationLevel.set(((LevelAccessorBridge) structureManager.level).bridge$getMinecraftWorld());
            try {
                DecorationOps.callsite().invoke(self, chunk, noise, random, biomes, possibleBiomes, rule);
            } finally {
                this.arclight$restoreGenerationLevel(previous);
            }
        }
    }

    @Decorate(method = "lambda$buildTerrain$0", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;zone(Ljava/lang/String;)Lnet/minecraft/util/profiling/Zone;", ordinal = 2), require = 1, allow = 1)
    private void arclight$generateSurface(@Local(ordinal = 0) ChunkAccess chunk) {
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        if (generator != null) {
            generator.buildSurface(chunk);
        }
    }

    @Decorate(method = "lambda$buildTerrain$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;generateCarvers(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V"), require = 1, allow = 1)
    private void arclight$shouldGenerateCarvers(NoiseBasedChunkGenerator self, ChunkAccess chunk, Blender blender,
            NoiseChunk noise, RandomState random, BiomeManager biomes, WorldGenRegion region, MaterialRule rule,
            @Local(ordinal = 0) StructureManager structureManager) throws Throwable {
        CustomChunkGenerator generator = this.arclight$customGenerator.get();
        if (generator == null || generator.shouldGenerateCaves(chunk)) {
            Level previous = this.arclight$generationLevel.get();
            this.arclight$generationLevel.set(((LevelAccessorBridge) structureManager.level).bridge$getMinecraftWorld());
            try {
                DecorationOps.callsite().invoke(self, chunk, blender, noise, random, biomes, region, rule);
            } finally {
                this.arclight$restoreGenerationLevel(previous);
            }
        }
    }

    @Decorate(method = "lambda$buildTerrain$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;close()V", ordinal = 0), require = 1, allow = 1)
    private void arclight$generateCarvers(NoiseChunk noise, @Local(ordinal = 0) ChunkAccess chunk) throws Throwable {
        Throwable failure = null;
        try {
            CustomChunkGenerator generator = this.arclight$customGenerator.get();
            if (generator != null) {
                generator.applyCarvers(chunk);
            }
        } catch (Throwable throwable) {
            failure = throwable;
        }
        try {
            DecorationOps.callsite().invoke(noise);
        } catch (Throwable throwable) {
            if (failure == null) {
                throw throwable;
            }
            failure.addSuppressed(throwable);
        }
        if (failure != null) {
            throw failure;
        }
    }
}
