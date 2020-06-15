package io.izzel.arclight.impl.mixin.v1_14.core.item;

import io.izzel.arclight.common.bridge.item.BlockItemBridge;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockItem.class)
public class BlockItemMixin_1_14 implements BlockItemBridge {

    @Override
    public boolean bridge$noCollisionInSel(World world, BlockState state, BlockPos pos, ISelectionContext context) {
        return world.func_217350_a(state, pos, context);
    }
}
