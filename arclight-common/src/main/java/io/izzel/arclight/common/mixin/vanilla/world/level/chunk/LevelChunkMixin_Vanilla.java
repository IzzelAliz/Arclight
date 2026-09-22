package io.izzel.arclight.common.mixin.vanilla.world.level.chunk;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public class LevelChunkMixin_Vanilla {

    // Match CraftBukkit's capture boundary; the commit path owns ordinary onPlace.
    // NeoForge has its own native captureBlockSnapshots gate and does not use this mixin.
    @Decorate(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"))
    private void arclight$deferCapturedOnPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) throws Throwable {
        if (!((WorldBridge) level).bridge$isCapturingBlockBreak() || state.getBlock() instanceof BaseEntityBlock) {
            DecorationOps.callsite().invoke(state, level, pos, previous, moving);
        }
    }
}
