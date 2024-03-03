package io.izzel.arclight.common.mixin.core.world.entity.animal.camel;

import io.izzel.arclight.common.mixin.core.world.entity.animal.horse.AbstractHorseMixin;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Camel.class)
public abstract class CamelMixin extends AbstractHorseMixin {

    // @formatter:off
    @Shadow public abstract void standUpInstantly();
    // @formatter:on

    @Override
    protected boolean damageEntity0(DamageSource damagesource, float f) {
        boolean hurt = super.damageEntity0(damagesource, f);
        if (!hurt) {
            return false;
        }
        this.standUpInstantly();
        return true;
    }
}
