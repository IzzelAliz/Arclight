package io.izzel.arclight.common.mixin.core.world.level;

import com.destroystokyo.paper.exception.ServerInternalException;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.NaturalSpawner;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerExceptionEventMixin {

    @Decorate(method = "getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;",
        require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V", remap = false))
    private static void arclight$naturalSpawnException(Logger logger, String message, Throwable cause) throws Throwable {
        DecorationOps.callsite().invoke(logger, message, cause);
        try {
            ServerInternalException.reportInternalException(cause);
        } catch (Throwable ignored) {
            // A failed reporting logger must not change the native recovery.
        }
    }

    @Decorate(method = "spawnMobsForChunkGeneration(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/util/RandomSource;)V",
        require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V", remap = false))
    private static void arclight$generationSpawnException(Logger logger, String message, Throwable cause) throws Throwable {
        DecorationOps.callsite().invoke(logger, message, cause);
        try {
            ServerInternalException.reportInternalException(cause);
        } catch (Throwable ignored) {
            // A failed reporting logger must not change the native recovery.
        }
    }
}
