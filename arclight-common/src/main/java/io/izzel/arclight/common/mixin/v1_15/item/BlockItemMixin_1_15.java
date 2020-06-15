package io.izzel.arclight.common.mixin.v1_15.item;

import io.izzel.arclight.common.bridge.item.BlockItemBridge;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockItem.class)
public class BlockItemMixin_1_15 implements BlockItemBridge {

    @Override
    public boolean bridge$noCollisionInSel(World world, BlockState state, BlockPos pos, ISelectionContext context) {
        return world.func_226663_a_(state, pos, context);
    }
}
