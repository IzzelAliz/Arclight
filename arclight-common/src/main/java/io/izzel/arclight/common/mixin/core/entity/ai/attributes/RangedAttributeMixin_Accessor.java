package io.izzel.arclight.common.mixin.core.entity.ai.attributes;

import io.izzel.arclight.common.bridge.entity.ai.attributes.RangedAttributeBridge;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin_Accessor implements RangedAttributeBridge {

    @Override @Accessor("maximumValue")
    public abstract void bridge$setMaximumValue(double maximumValue);
}
