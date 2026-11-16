package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.world.entity.monster.cubemob.Slime.class)
public abstract class SlimeMixin extends MobMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = "checkSlimeSpawnRules(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;seedSlimeChunk(IIJJ)Lnet/minecraft/util/RandomSource;"), require = 1)
    private static net.minecraft.util.RandomSource arclight$slimeSeed(int x, int z, long worldSeed, long salt, Operation<net.minecraft.util.RandomSource> original, @Local(argsOnly = true) net.minecraft.world.level.LevelAccessor level) {
        var world = (io.izzel.arclight.common.bridge.core.world.level.WorldBridge) ((net.minecraft.world.level.WorldGenLevel) level).getLevel();
        return original.call(x, z, worldSeed, (long) world.bridge$spigotConfig().slimeSeed);
    }

    @ModifyVariable(
        method = "checkSlimeSpawnRules(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z",
        at = @At("STORE"), ordinal = 0, require = 1, allow = 1)
    private static boolean arclight$allChunksAreSlimeChunks(boolean slimeChunk, @Local(argsOnly = true) net.minecraft.world.level.LevelAccessor level) {
        var world = (io.izzel.arclight.common.bridge.core.world.level.WorldBridge) ((net.minecraft.world.level.WorldGenLevel) level).getLevel();
        return world.bridge$paperConfig().entities.spawning.allChunksAreSlimeChunks || slimeChunk;
    }
}
