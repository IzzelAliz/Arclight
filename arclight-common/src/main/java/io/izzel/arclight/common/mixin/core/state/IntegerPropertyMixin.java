package io.izzel.arclight.common.mixin.core.state;

import net.minecraft.state.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegerProperty.class)
public class IntegerPropertyMixin {

    public int min;
    public int max;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$setValue(String name, int min, int max, CallbackInfo ci) {
        this.max = max;
        this.min = min;
    }
}
