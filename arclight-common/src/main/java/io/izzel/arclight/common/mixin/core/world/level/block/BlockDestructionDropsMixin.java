package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockDestructionBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockDestructionDropsMixin implements BlockDestructionBridge {

    @Decorate(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;"), require = 1, allow = 1)
    private static List<ItemStack> arclight$editBlockBreakDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity) throws Throwable {
        List<ItemStack> drops = (List<ItemStack>) DecorationOps.callsite().invoke(state, level, pos, blockEntity);
        BlockDestruction context = BlockDestruction.current(level, pos);
        // NeoForge dispatches at handleBlockDrops instead, preserving the loader event and XP hook.
        if (context != null && context.isBlockBreak()) {
            if (((BlockDestructionBridge) state.getBlock()).bridge$usesNeoForgeDestructionDrops()) {
                context.captureNeoForgeLoot(drops);
            } else {
                return context.editVanillaDrops(state, level, pos, drops);
            }
        }
        return drops;
    }
}
