package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import io.izzel.arclight.common.bridge.util.WeightedListBridge;
import net.minecraft.entity.ai.brain.task.MultiTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.util.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiTask.class)
public class MultiTaskMixin<E> {

    @Shadow @Final private WeightedList<Task<? super E>> field_220419_e;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$setUnsafe(CallbackInfo ci) {
        ((WeightedListBridge) this.field_220419_e).bridge$setUnsafe(false);
    }
}
