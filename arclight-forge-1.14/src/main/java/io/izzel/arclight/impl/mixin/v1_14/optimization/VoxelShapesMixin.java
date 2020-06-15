package io.izzel.arclight.impl.mixin.v1_14.optimization;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.shapes.DoubleCubeMergingList;
import net.minecraft.util.math.shapes.DoubleRangeList;
import net.minecraft.util.math.shapes.IDoubleListMerger;
import net.minecraft.util.math.shapes.IndirectMerger;
import net.minecraft.util.math.shapes.NonOverlappingMerger;
import net.minecraft.util.math.shapes.SimpleDoubleMerger;
import net.minecraft.util.math.shapes.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(VoxelShapes.class)
public abstract class VoxelShapesMixin {

    // @formatter:off
    @Shadow protected static long lcm(int aa, int bb) { return 0; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @VisibleForTesting
    @Overwrite
    protected static IDoubleListMerger makeListMerger(final int i, final DoubleList doublelist, final DoubleList doublelist1, final boolean flag, final boolean flag1) {
        if (doublelist.getDouble(0) == Double.NEGATIVE_INFINITY && doublelist.getDouble(doublelist.size() - 1) == Double.POSITIVE_INFINITY) {
            return new IndirectMerger(doublelist, doublelist1, flag, flag1);
        }
        int j = doublelist.size() - 1;
        int k = doublelist1.size() - 1;
        if (doublelist instanceof DoubleRangeList && doublelist1 instanceof DoubleRangeList) {
            long l = lcm(j, k);
            if (i * l <= 256L) {
                return new DoubleCubeMergingList(j, k);
            }
        }
        if (j == k && Objects.equals(doublelist, doublelist1)) {
            if (doublelist instanceof SimpleDoubleMerger) {
                return (SimpleDoubleMerger) doublelist;
            }
            if (doublelist1 instanceof SimpleDoubleMerger) {
                return (SimpleDoubleMerger) doublelist1;
            }
            return new SimpleDoubleMerger(doublelist);
        } else {
            if (doublelist.getDouble(j) < doublelist1.getDouble(0) - 1.0E-7) {
                return new NonOverlappingMerger(doublelist, doublelist1, false);
            }
            if (doublelist1.getDouble(k) < doublelist.getDouble(0) - 1.0E-7) {
                return new NonOverlappingMerger(doublelist1, doublelist, true);
            }
            return new IndirectMerger(doublelist, doublelist1, flag, flag1);
        }
    }
}
