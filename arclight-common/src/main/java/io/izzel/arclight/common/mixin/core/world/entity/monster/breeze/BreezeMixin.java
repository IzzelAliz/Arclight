package io.izzel.arclight.common.mixin.core.world.entity.monster.breeze;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.breeze.Breeze;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Breeze.class)
public abstract class BreezeMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void arclight$brainTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity brainTarget = ((Breeze) (Object) this).getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (brainTarget != null) {
            cir.setReturnValue(target.is(brainTarget));
        }
    }
}
