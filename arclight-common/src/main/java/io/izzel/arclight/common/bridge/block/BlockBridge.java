package io.izzel.arclight.common.bridge.block;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface BlockBridge {

    int bridge$getExpDrop(BlockState blockState, World world, BlockPos blockPos, ItemStack itemStack);
}
