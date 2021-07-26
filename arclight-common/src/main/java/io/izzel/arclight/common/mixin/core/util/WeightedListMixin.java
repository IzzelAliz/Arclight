package io.izzel.arclight.common.mixin.core.util;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.util.WeightedListBridge;
import net.minecraft.util.WeightedList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Fix concurrency issue in WeightedList
 *     @see WeightedList
 */
@Mixin(WeightedList.class)
public class WeightedListMixin<U> implements WeightedListBridge {

    // @formatter:off
    @Shadow @Final protected List<WeightedList.Entry<U>> weightedEntries;
    // @formatter:on

    @Unique private boolean isUnsafe;

    @Inject(method = "<init>()V", at = @At("TAIL"))
    public void arclight$constructor(CallbackInfo ci) {
        this.isUnsafe = true;
    }

    @Inject(method = "<init>(Ljava/util/List;)V", at = @At("TAIL"))
    public void arclight$constructor(List<WeightedList.Entry<U>> weightedEntries, CallbackInfo ci) {
        this.isUnsafe = true;
    }

    public WeightedList<U> getWeightedList(List<WeightedList.Entry<U>> weightedEntries, boolean isUnsafe) {
        WeightedList<U> weightedList = new WeightedList<U>(weightedEntries);
        ((WeightedListBridge) weightedList).bridge$setUnsafe(isUnsafe);
        return weightedList;
    }

    /**
     * @author yuesha-yc
     * @reason Re-write this method to work on a clone of entries for concurrency
     */
    @Overwrite()
    public WeightedList<U> randomizeWithWeight(Random rand) {
        List<WeightedList.Entry<U>> list = this.isUnsafe ? Lists.newArrayList(this.weightedEntries) : this.weightedEntries;
        list.forEach(entry -> entry.generateChance(rand.nextFloat()));
        list.sort(Comparator.comparingDouble(WeightedList.Entry::getChance));
        WeightedListBridge weightedList = this;
        return this.isUnsafe ? getWeightedList(list, this.isUnsafe) : (WeightedList<U>) weightedList;
    }

    @Override
    public void bridge$setUnsafe(boolean unsafe) {
        isUnsafe = unsafe;
    }

    @Override
    public boolean bridge$getIsUnsafe() {
        return isUnsafe;
    }
}
