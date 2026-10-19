package io.izzel.arclight.common.mixin.core.world.entity.ai.targeting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public class TargetingConditionsMixin {

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void arclight$sameWorld(ServerLevel level, LivingEntity targeter, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && targeter != null && targeter.level() != target.level()) {
            cir.setReturnValue(false);
        }
    }
}
