package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VegetationBlock.class)
public abstract class BushBlockMixin extends BlockMixin {

    @Redirect(method = "updateShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState arclight$blockFade(Block block, BlockState state, LevelReader level,
                                         ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction,
                                         BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (!((Object) this instanceof BushBlock)) {
            return block.defaultBlockState();
        }
        if (!(level instanceof net.minecraft.world.level.LevelAccessor levelAccessor)) {
            return block.defaultBlockState();
        }
        if (!CraftEventFactory.callBlockPhysicsEvent(levelAccessor, pos).isCancelled()) {
            return block.defaultBlockState();
        } else {
            return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        }
    }
}
