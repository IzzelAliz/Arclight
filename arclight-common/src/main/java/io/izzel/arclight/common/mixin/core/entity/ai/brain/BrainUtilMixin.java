package io.izzel.arclight.common.mixin.core.entity.ai.brain;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.BrainUtil;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrainUtil.class)
public class BrainUtilMixin {

    @Inject(method = "throwItemAt", cancellable = true, at = @At("HEAD"))
    private static void arclight$noEmptyLoot(LivingEntity from, ItemStack stack, LivingEntity to, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }
}
