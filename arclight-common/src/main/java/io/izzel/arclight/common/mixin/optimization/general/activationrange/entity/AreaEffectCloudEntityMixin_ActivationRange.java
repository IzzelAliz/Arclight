package io.izzel.arclight.common.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public int waitTime;
    @Shadow private int duration;
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (this.tickCount >= this.waitTime + this.duration) {
            this.discard();
        }
    }
}
