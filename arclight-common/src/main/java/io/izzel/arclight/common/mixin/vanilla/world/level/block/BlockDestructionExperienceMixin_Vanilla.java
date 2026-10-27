package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class BlockDestructionExperienceMixin_Vanilla {

    @Decorate(method = {
        "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
        "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"), require = 2, expect = 2, allow = 2)
    private static void arclight$editedDestructionExperience(BlockState state, ServerLevel level, BlockPos pos,
                                                           ItemStack tool, boolean dropExperience) throws Throwable {
        BlockDestruction context = BlockDestruction.current(level, pos);
        boolean selectedExperience = context == null && dropExperience;
        DecorationOps.callsite().invoke(state, level, pos, tool, selectedExperience);
        if (context != null) {
            state.getBlock().popExperience(level, pos, context.experience());
        }
    }
}
