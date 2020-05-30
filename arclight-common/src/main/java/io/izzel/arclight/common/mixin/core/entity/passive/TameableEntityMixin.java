package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.TameableEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TameableEntity.class)
public abstract class TameableEntityMixin extends AnimalEntityMixin {

    // @formatter:off
    @Shadow public abstract boolean isTamed();
    // @formatter:on
}
