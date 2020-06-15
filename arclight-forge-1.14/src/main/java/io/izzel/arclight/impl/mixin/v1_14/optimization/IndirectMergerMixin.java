package io.izzel.arclight.impl.mixin.v1_14.optimization;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.math.shapes.IDoubleListMerger;
import net.minecraft.util.math.shapes.IndirectMerger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IndirectMerger.class)
public class IndirectMergerMixin {

    private static final IntArrayList INFINITE_B_1 = new IntArrayList(new int[]{1, 1});
    private static final IntArrayList INFINITE_B_0 = new IntArrayList(new int[]{0, 0});
    private static final IntArrayList INFINITE_C = new IntArrayList(new int[]{0, 1});

    @Shadow @Final @Mutable private IntArrayList list1;
    @Shadow @Final @Mutable private IntArrayList list2;

    private DoubleList list;

    public void arclight$constructor$override(DoubleList list1In, DoubleList list2In, boolean p_i47685_3_, boolean p_i47685_4_) {
        int i = 0;
        int j = 0;
        double d0 = Double.NaN;
        int k = list1In.size();
        int l = list2In.size();
        int i1 = k + l;

        int size = list1In.size();
        double tail = list1In.getDouble(size - 1);
        double head = list1In.getDouble(0);
        if (head == Double.NEGATIVE_INFINITY && tail == Double.POSITIVE_INFINITY && !p_i47685_3_ && !p_i47685_4_ && (size == 2 || size == 4)) {
            this.list = list2In;
            if (size == 2) {
                this.list1 = INFINITE_B_0;
            } else {
                this.list1 = INFINITE_B_1;
            }
            this.list2 = INFINITE_C;
            return;
        }

        this.list = new DoubleArrayList(i1);
        this.list1 = new IntArrayList(i1);
        this.list2 = new IntArrayList(i1);

        while (true) {
            boolean flag = i < k;
            boolean flag1 = j < l;
            if (!flag && !flag1) {
                if (this.list.isEmpty()) {
                    this.list.add(Math.min(list1In.getDouble(k - 1), list2In.getDouble(l - 1)));
                }

                return;
            }

            boolean flag2 = flag && (!flag1 || list1In.getDouble(i) < list2In.getDouble(j) + 1.0E-7D);
            double d1 = flag2 ? list1In.getDouble(i++) : list2In.getDouble(j++);

            if ((i != 0 && flag || flag2 || p_i47685_4_) && (j != 0 && flag1 || !flag2 || p_i47685_3_)) {
                if (!(d0 >= d1 - 1.0E-7D)) {
                    this.list1.add(i - 1);
                    this.list2.add(j - 1);
                    this.list.add(d1);
                    d0 = d1;
                } else if (!this.list.isEmpty()) {
                    this.list1.set(this.list1.size() - 1, i - 1);
                    this.list2.set(this.list2.size() - 1, j - 1);
                }
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean forMergedIndexes(IDoubleListMerger.IConsumer consumer) {
        for (int i = 0; i < this.list.size() - 1; ++i) {
            if (!consumer.merge(this.list1.getInt(i), this.list2.getInt(i), i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public DoubleList func_212435_a() {
        return this.list;
    }
}
