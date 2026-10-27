package io.izzel.arclight.common.mixin.core.world.entity.animal.nautilus;

import io.izzel.arclight.common.mixin.core.world.entity.TamableAnimalMixin;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Nautilus.class)
public abstract class NautilusMixin extends TamableAnimalMixin {

    @Inject(method = "getMaxAirSupply()I", at = @At("RETURN"), cancellable = true, require = 1)
    private void arclight$useBukkitMaxAir(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.maxAirTicks);
    }

    @ModifyConstant(method = "handleAirSupply(Lnet/minecraft/server/level/ServerLevel;I)V",
        constant = @Constant(intValue = 300), require = 1, expect = 1, allow = 1)
    private int arclight$restoreMaxAir(int original) {
        return this.getMaxAirSupply();
    }
}
