package io.izzel.arclight.common.bridge.core.world.level;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

public interface LevelAccessorBridge {
    default net.minecraft.world.level.block.state.BlockState getBlockStateIfLoaded(net.minecraft.core.BlockPos pos) {
        return this.bridge$getBlockStateIfLoaded(pos);
    }

    default net.minecraft.world.level.material.FluidState getFluidIfLoaded(net.minecraft.core.BlockPos pos) {
        return this.bridge$getFluidIfLoaded(pos);
    }

    default net.minecraft.world.level.chunk.ChunkAccess getChunkIfLoadedImmediately(int x, int z) {
        return this.bridge$getChunkIfLoadedImmediately(x, z);
    }

    default net.minecraft.world.level.block.state.BlockState bridge$getBlockStateIfLoaded(net.minecraft.core.BlockPos pos) {
        var chunk = this.getChunkIfLoadedImmediately(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    default net.minecraft.world.level.material.FluidState bridge$getFluidIfLoaded(net.minecraft.core.BlockPos pos) {
        var chunk = this.getChunkIfLoadedImmediately(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getFluidState(pos);
    }

    default net.minecraft.world.level.chunk.ChunkAccess bridge$getChunkIfLoadedImmediately(int x, int z) {
        throw new UnsupportedOperationException("Immediate chunk access is not supported by " + this.getClass().getName());
    }

    default ServerLevel getMinecraftWorld() {
        return this.bridge$getMinecraftWorld();
    }

    static LevelAccessorBridge from(LevelAccessor world) {
        final var result = (LevelAccessorBridge) world;
        return result.arclight$isActual() ? result : null;
    }

    default boolean arclight$isActual() {
        return DistValidate.isValid((LevelAccessor) this);
    }

    default ServerLevel bridge$getMinecraftWorld() {
        throw new UnsupportedOperationException(String.format("No server level found for %s.\n This is likely because the specified world is not a ServerLevelAccessor and thus it shouldn't be a logic world.\n Otherwise it is a bug of Arclight.", getClass().getName()));
    }
}
