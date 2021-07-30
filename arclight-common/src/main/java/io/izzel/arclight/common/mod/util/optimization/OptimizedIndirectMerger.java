package io.izzel.arclight.common.mod.util.optimization;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.phys.shapes.IndexMerger;
import org.jetbrains.annotations.NotNull;

/**
 * @author jellysquid3 (LGPLv3)
 */
public class OptimizedIndirectMerger implements IndexMerger {

    private final double[] merged;
    private final int[] indicesFirst;
    private final int[] indicesSecond;

    private final DoubleArrayList pairs;

    public OptimizedIndirectMerger(DoubleList aPoints, DoubleList bPoints, boolean flag1, boolean flag2) {
        double[] araw;

        if (aPoints instanceof DoubleArrayList) {
            araw = ((DoubleArrayList) aPoints).elements();
        } else {
            araw = new double[aPoints.size()];

            for (int i = 0; i < araw.length; i++) {
                araw[i] = aPoints.getDouble(i);
            }
        }

        double[] braw;

        if (bPoints instanceof DoubleArrayList) {
            braw = ((DoubleArrayList) bPoints).elements();
        } else {
            braw = new double[bPoints.size()];

            for (int i = 0; i < braw.length; i++) {
                braw[i] = bPoints.getDouble(i);
            }
        }

        int size = araw.length + braw.length;

        this.merged = new double[size];
        this.indicesFirst = new int[size];
        this.indicesSecond = new int[size];

        this.pairs = DoubleArrayList.wrap(this.merged);

        this.merge(araw, braw, araw.length, braw.length, flag1, flag2);
    }

    private void merge(double[] aPoints, double[] bPoints, int aSize, int bSize, boolean flag1, boolean flag2) {
        int aIdx = 0;
        int bIdx = 0;

        double prev = 0.0D;

        int a1 = 0, a2 = 0;

        while (true) {
            boolean aWithinBounds = aIdx < aSize;
            boolean bWithinBounds = bIdx < bSize;

            if (!aWithinBounds && !bWithinBounds) {
                break;
            }

            boolean flip = aWithinBounds && (!bWithinBounds || aPoints[aIdx] < bPoints[bIdx] + 1.0E-7D);

            double value;

            if (flip) {
                value = aPoints[aIdx++];
            } else {
                value = bPoints[bIdx++];
            }

            if ((aIdx == 0 || !aWithinBounds) && !flip && !flag2) {
                continue;
            }

            if ((bIdx == 0 || !bWithinBounds) && flip && !flag1) {
                continue;
            }

            if (a2 == 0 || prev < value - 1.0E-7D) {
                this.indicesFirst[a1] = aIdx - 1;
                this.indicesSecond[a1] = bIdx - 1;
                this.merged[a2] = value;

                a1++;
                a2++;
                prev = value;
            } else if (a2 > 0) {
                this.indicesFirst[a1 - 1] = aIdx - 1;
                this.indicesSecond[a1 - 1] = bIdx - 1;
            }
        }

        if (a2 == 0) {
            this.merged[a2++] = Math.min(aPoints[aSize - 1], bPoints[bSize - 1]);
        }

        this.pairs.size(a2);
    }

    @Override
    public @NotNull DoubleList getList() {
        return this.pairs;
    }

    @Override
    public boolean forMergedIndexes(@NotNull IndexConsumer consumer) {
        int l = this.pairs.size() - 1;

        for (int i = 0; i < l; i++) {
            if (!consumer.merge(this.indicesFirst[i], this.indicesSecond[i], i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int size() {
        return this.pairs.size();
    }
}
