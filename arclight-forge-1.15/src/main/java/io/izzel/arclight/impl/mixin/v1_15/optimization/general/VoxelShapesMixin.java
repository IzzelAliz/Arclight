package io.izzel.arclight.impl.mixin.v1_15.optimization.general;

import io.izzel.arclight.impl.common.optimization.OptimizedIndirectMerger;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.shapes.IDoubleListMerger;
import net.minecraft.util.math.shapes.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VoxelShapes.class)
public class VoxelShapesMixin {

    @Inject(method = "makeListMerger", at = @At(value = "NEW", target = "net/minecraft/util/math/shapes/IndirectMerger", shift = At.Shift.BEFORE), cancellable = true)
    private static void arclight$optimizedMerger(int p_199410_0_, DoubleList list1, DoubleList list2, boolean p_199410_3_, boolean p_199410_4_, CallbackInfoReturnable<IDoubleListMerger> cir) {
        cir.setReturnValue(new OptimizedIndirectMerger(list1, list2, p_199410_3_, p_199410_4_));
    }
}
