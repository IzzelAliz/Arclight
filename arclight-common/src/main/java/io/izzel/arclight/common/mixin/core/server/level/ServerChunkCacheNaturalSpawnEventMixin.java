package io.izzel.arclight.common.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.server.level.NaturalSpawnSelectionBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheNaturalSpawnEventMixin {

    @Shadow @Final public ChunkMap chunkMap;
    @Shadow public boolean spawnEnemies;

    @WrapMethod(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", require = 1, allow = 1)
    private void arclight$naturalSpawnEventLifetime(ProfilerFiller profiler, long timeDiff, Operation<Void> original) {
        var selection = (NaturalSpawnSelectionBridge) this.chunkMap;
        selection.bridge$clearNaturalSpawnEvents();
        try {
            original.call(profiler, timeDiff);
        } finally {
            selection.bridge$clearNaturalSpawnEvents();
        }
    }

    @Decorate(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;", ordinal = 1),
        require = 1, allow = 1)
    private Object arclight$prepareNaturalSpawnEvents(GameRules rules, GameRule<?> rule,
                                                     @Local(ordinal = 0) boolean doMobSpawning) throws Throwable {
        Object tickSpeed = DecorationOps.callsite().invoke(rules, rule);
        // RANDOM_TICK_SPEED is the second read, after doMobSpawning has been
        // settled and before persistent/category calculations on both loaders.
        // The existing no-player redirect owns only the first rules read.
        if (doMobSpawning && (this.spawnEnemies || ((ServerChunkProviderBridge) this).bridge$getSpawnFriendlies())) {
            ((NaturalSpawnSelectionBridge) this.chunkMap).bridge$prepareNaturalSpawnEvents();
        }
        return tickSpeed;
    }
}
