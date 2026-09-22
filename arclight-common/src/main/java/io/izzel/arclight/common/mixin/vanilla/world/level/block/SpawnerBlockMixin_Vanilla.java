package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin_Vanilla extends BlockMixin_Vanilla {

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        RandomSource random = world.getRandom();
        return 15 + random.nextInt(15) + random.nextInt(15);
    }
}
