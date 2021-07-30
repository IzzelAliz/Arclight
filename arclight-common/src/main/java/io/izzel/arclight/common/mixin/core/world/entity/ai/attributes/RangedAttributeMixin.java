package io.izzel.arclight.common.mixin.core.world.entity.ai.attributes;

import io.izzel.arclight.common.bridge.core.entity.ai.attributes.RangedAttributeBridge;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin extends Attribute implements RangedAttributeBridge {

    // @formatter:off
    @Override @Accessor("maxValue") public abstract void bridge$setMaximumValue(double maximumValue);
    // @formatter:on

    protected RangedAttributeMixin(String attributeName, double defaultValue) {
        super(attributeName, defaultValue);
    }

    @Inject(method = "sanitizeValue", cancellable = true, at = @At("HEAD"))
    private void arclight$notNan(double value, CallbackInfoReturnable<Double> cir) {
        if (Double.isNaN(value)) {
            cir.setReturnValue(this.getDefaultValue());
        }
    }
}
