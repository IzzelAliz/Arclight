package io.izzel.arclight.common.mixin.vanilla.world.level;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.v.block.CapturedBlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Level.class, priority = 450)
public abstract class LevelMixin_Vanilla implements LevelAccessor, WorldBridge {

    // @formatter:off
    @Shadow @Final public boolean isClientSide;
    @Shadow public abstract boolean isDebug();
    @Shadow public abstract LevelChunk getChunkAt(BlockPos blockPos);
    @Shadow public abstract BlockState getBlockState(BlockPos blockPos);
    @Shadow public abstract void setBlocksDirty(BlockPos blockPos, BlockState blockState, BlockState blockState2);
    @Shadow public abstract void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i);
    @Shadow public abstract void updateNeighbourForOutputSignal(BlockPos blockPos, Block block);
    @Shadow public abstract void onBlockStateChange(BlockPos blockPos, BlockState blockState, BlockState blockState2);
    // @formatter:on

    public boolean captureBlockStates = false;

    @Override
    public void bridge$platform$startCaptureBlockBreak() {
        this.captureBlockStates = true;
    }

    @Override
    public boolean bridge$isCapturingBlockBreak() {
        return this.captureBlockStates;
    }

    @Override
    public void bridge$platform$endCaptureBlockBreak() {
        this.captureBlockStates = false;
    }

    @Inject(method = "setBlockEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;addAndRegisterBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void arclight$captureBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (this.bridge$isCapturingBlockBreak()) {
            this.bridge$getCapturedBlockEntity().put(blockEntity.getBlockPos().immutable(), blockEntity);
            ci.cancel();
        }
    }

    @Inject(method = "getBlockEntity", cancellable = true, at = @At(value = "HEAD"))
    private void arclight$getCaptureBlockEntity(BlockPos blockPos, CallbackInfoReturnable<BlockEntity> cir) {
        if (this.bridge$getCapturedBlockEntity().containsKey(blockPos)) {
            cir.setReturnValue(this.bridge$getCapturedBlockEntity().get(blockPos));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
        if (this.isOutsideBuildHeight(blockPos)) {
            return false;
        } else if (!this.isClientSide && this.isDebug()) {
            return false;
        } else {
            LevelChunk levelChunk = this.getChunkAt(blockPos);
            Block block = blockState.getBlock();

            // CraftBukkit start - capture blockstates
            if (this.captureBlockStates && !this.bridge$getCapturedBlockState().containsKey(blockPos)) {
                var blockstate = CapturedBlockState.getBlockState((Level) (Object) this, blockPos, i);
                this.bridge$getCapturedBlockState().put(blockPos.immutable(), blockstate);
            }
            // CraftBukkit end

            BlockState blockState2 = levelChunk.setBlockState(blockPos, blockState, (i & 64) != 0);
            if (blockState2 == null) {
                // CraftBukkit start - remove blockstate if failed (or the same)
                if (this.captureBlockStates) {
                    this.bridge$getCapturedBlockState().remove(blockPos);
                }
                // CraftBukkit end
                return false;
            } else {
                if (this.captureBlockStates) {
                    return true;
                }

                BlockState blockState3 = this.getBlockState(blockPos);
                if (blockState3 == blockState) {
                    if (blockState2 != blockState3) {
                        this.setBlocksDirty(blockPos, blockState2, blockState3);
                    }

                    if ((i & 2) != 0 && (!this.isClientSide || (i & 4) == 0) && (this.isClientSide || levelChunk.getFullStatus() != null && levelChunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                        this.sendBlockUpdated(blockPos, blockState2, blockState, i);
                    }

                    if ((i & 1) != 0) {
                        this.blockUpdated(blockPos, blockState2.getBlock());
                        if (!this.isClientSide && blockState.hasAnalogOutputSignal()) {
                            this.updateNeighbourForOutputSignal(blockPos, block);
                        }
                    }

                    if ((i & 16) == 0 && j > 0) {
                        int k = i & -34;
                        blockState2.updateIndirectNeighbourShapes(this, blockPos, k, j - 1);
                        blockState.updateNeighbourShapes(this, blockPos, k, j - 1);
                        blockState.updateIndirectNeighbourShapes(this, blockPos, k, j - 1);
                    }

                    this.onBlockStateChange(blockPos, blockState2, blockState3);
                }

                return true;
            }
        }
    }
}
