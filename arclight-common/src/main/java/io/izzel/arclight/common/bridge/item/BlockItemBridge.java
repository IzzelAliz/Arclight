package io.izzel.arclight.common.bridge.item;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.World;

public interface BlockItemBridge {

    boolean bridge$noCollisionInSel(World world, BlockState state, BlockPos pos, ISelectionContext context);
}
