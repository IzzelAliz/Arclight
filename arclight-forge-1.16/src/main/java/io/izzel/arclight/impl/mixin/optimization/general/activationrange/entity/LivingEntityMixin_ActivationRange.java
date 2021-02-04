package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow protected int idleTime;
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        this.idleTime++;
    }
}
