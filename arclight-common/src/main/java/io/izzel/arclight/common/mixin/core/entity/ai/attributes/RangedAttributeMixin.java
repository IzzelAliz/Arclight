package io.izzel.arclight.common.mixin.core.entity.ai.attributes;

import io.izzel.arclight.common.bridge.entity.ai.attributes.RangedAttributeBridge;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin extends Attribute implements RangedAttributeBridge {

    protected RangedAttributeMixin(@Nullable IAttribute parentIn, String unlocalizedNameIn, double defaultValueIn) {
        super(parentIn, unlocalizedNameIn, defaultValueIn);
    }

    // @formatter:off
    @Override @Accessor("maximumValue") public abstract void bridge$setMaximumValue(double maximumValue);
    // @formatter:on

    @Inject(method = "clampValue", cancellable = true, at = @At("HEAD"))
    private void arclight$notNan(double value, CallbackInfoReturnable<Double> cir) {
        if (Double.isNaN(value)) {
            cir.setReturnValue(this.getDefaultValue());
        }
    }
}
