package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.AreaEffectCloudEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AreaEffectCloudEntity.class)
public abstract class AreaEffectCloudEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public int waitTime;
    @Shadow private int duration;
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (this.ticksExisted >= this.waitTime + this.duration) {
            this.remove();
        }
    }
}
